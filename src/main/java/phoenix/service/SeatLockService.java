package phoenix.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phoenix.model.dto.ReservationsDto;
import phoenix.model.mapper.SeatsMapper;
import phoenix.util.RedisKeys;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class SeatLockService {

    // 의존성 주입
    private final SeatsMapper seatsMapper;
    private final RedissonClient redisson;
    private final SeatCsvService seatCsvService; // ⬅️ CSV 메타 (시니어석 판별용)
    private final GameService gameService;       // ⬅️ 경기 시작 시각 조회(D-2 계산)
    private final TicketsService ticketsService;

    // 상수 정의
    private static final long HOLD_TTL_SECONDS = 120;
    private static final int  MAX_SEATS_PER_USER = 4;
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    // =========================
    // 내부 Redis 접근자
    // =========================
    private RMapCache<String, String> holdMap() { return redisson.getMapCache(RedisKeys.SEAT_HOLD_MAP); }
    private RSet<Integer> soldSet(int gno)       { return redisson.getSet(RedisKeys.SEAT_SOLD_SET + ":" + gno); }
    private RSetCache<Integer> userHoldSet(int mno, int gno) { return redisson.getSetCache("user:hold:" + mno + ":" + gno); }
    private String seatKey(int gno, int sno)     { return gno + ":" + sno; }
    private RLock seatLock(int gno, int sno)     { return redisson.getLock("seat:lock:" + seatKey(gno, sno)); }

    // =========================
    // 게이트/중복 가드 (변경 없음)
    // =========================
    private boolean hasActiveSession(int mno, int gno) {
        boolean scoped = redisson.getBucket(RedisKeys.keySession(gno, mno)).isExists();
        boolean legacy = redisson.getBucket(RedisKeys.SESSION_PREFIX + mno).isExists();
        return scoped || legacy;
    }

    // =========================
    // 서버 기동 시 SOLD 복구 (변경 없음)
    // =========================
    @PostConstruct
    public void initSoldFromDb() {
        try {
            // 1️⃣ SOLD 복구 (기존 로직)
            List<Integer> gnos = seatsMapper.findAllGnosHavingReserved();
            if (gnos == null || gnos.isEmpty()) {
                System.out.println("[SeatLockService] SOLD 복구: reserved 데이터 없음");
                return;
            }
            int sets = 0, seats = 0;
            for (int gno : gnos) {
                List<Integer> snos = seatsMapper.findReservedSnosByGno(gno);
                RSet<Integer> sold = soldSet(gno);
                if (sold.isEmpty() && snos != null && !snos.isEmpty()) {
                    sold.addAll(snos);
                    sets++;
                    seats += snos.size();
                }
            }

            // 2️⃣ 유저별 예매 카운터 복구 (신규)
            List<ReservationsDto> userSummary = seatsMapper.findUserReservedCountSummary();
            int restoredUsers = 0;
            for (ReservationsDto dto : userSummary) {
                String key = "user_booking_count:" + dto.getMno() + ":" + dto.getGno();
                RAtomicLong counter = redisson.getAtomicLong(key);
                counter.set(dto.getCount());
                counter.expire(6, TimeUnit.HOURS);
                restoredUsers++;
            }

            System.out.printf("[SeatLockService] SOLD 복구 완료 (sets=%d, seats=%d, userCounts=%d)%n",
                    sets, seats, restoredUsers);

        } catch (Exception e) {
            System.out.println("[SeatLockService] SOLD 복구 실패: " + e.getMessage());
        }
    }


    // =========================
    // ⏰ 일반예매용 시니어석 오픈 판정 (D-2)
    // =========================
    /**
     * 일반 예매 기준: 시니어석은 경기 시작 2일 전부터만 오픈.
     * - games.csv → GameService → date/time 이용
     * - date/time 미존재 시 보수적으로 '미오픈' 처리
     */
    private boolean isSeniorOpenForGeneral(int gno) {
        var game = gameService.findByGno(gno);
        if (game == null || game.getDate() == null || game.getTime() == null) return false;
        ZonedDateTime startAt = ZonedDateTime.of(game.getDate(), game.getTime(), ZONE_SEOUL);
        ZonedDateTime gate = startAt.minusDays(2);
        return !ZonedDateTime.now(ZONE_SEOUL).isBefore(gate); // now >= gate
    }

    // =========================
    // 좌석 선택(락) — 수동/자동 공통 진입점
    // =========================
    /**
     * 좌석 선택 시도
     * @return
     *   1  : OK
     *  -1  : 세션 없음
     *  -3  : 매진/락선점 실패
     *  -4  : 한도(4개) 초과
     *  -5  : INVALID_SEAT (sno가 zno에 속하지 않음)
     *  -6  : SENIOR_NOT_OPEN (시니어석은 일반예매에서 D-2부터만 허용)
     */
    public int tryLockSeat(int mno, int gno, int zno, int sno) throws InterruptedException {

        if (!hasActiveSession(mno, gno)) return -1;
        if (!seatCsvService.existsSeatInZone(zno, sno)) return -5;
        if (seatCsvService.isSeniorSeat(sno) && !isSeniorOpenForGeneral(gno)) return -6;
        if (soldSet(gno).contains(sno)) return -3;

        RSetCache<Integer> myHolds = userHoldSet(mno, gno);
        int holdCount = myHolds.size();

        // ✅ Redis에서 예매완료 좌석 수 읽기
        RAtomicLong bookedCount = redisson.getAtomicLong("user_booking_count:" + mno + ":" + gno);
        int confirmedCount = (int) bookedCount.get();

        // ✅ (확정 + 홀드) ≥ 4 → 제한
        if (confirmedCount + holdCount >= MAX_SEATS_PER_USER) return -4;

        RLock lock = seatLock(gno, sno);
        if (!lock.tryLock(0, HOLD_TTL_SECONDS, TimeUnit.SECONDS)) return -3;

        holdMap().put(seatKey(gno, sno), String.valueOf(mno), HOLD_TTL_SECONDS, TimeUnit.SECONDS);
        myHolds.add(sno, HOLD_TTL_SECONDS, TimeUnit.SECONDS);
        return 1;
    }


    // =========================
    // 좌석 해제 (변경 없음)
    // =========================
    /**
     * 내 임시 좌석 해제
     */
    public boolean releaseSeat(int mno, int gno, int zno, int sno) {
        if (!seatCsvService.existsSeatInZone(zno, sno)) return false;
        String key = seatKey(gno, sno);
        String holder = holdMap().get(key);
        if (holder == null || !holder.equals(String.valueOf(mno))) return false;

        holdMap().remove(key);
        userHoldSet(mno, gno).remove(sno);
        try { seatLock(gno, sno).forceUnlock(); } catch (Exception ignore) {}
        return true;
    }

    // =========================
    // 결제 확정 (변경 없음)
    // =========================
    /**
     * 임시 보유 좌석들을 결제로 확정 (SOLD 세트 반영)
     */
    public boolean confirmSeats(int mno, int gno, List<Integer> snos, StringBuilder failReason) {
        if (!hasActiveSession(mno, gno)) { failReason.append("no session"); return false; }
        if (snos == null || snos.isEmpty()) { failReason.append("empty"); return false; }

        RSet<Integer> sold = soldSet(gno);
        for (int sno : snos) {
            if (sold.contains(sno)) { failReason.append(sno).append(" sold; "); return false; }
        }
        RMapCache<String, String> holds = holdMap();
        for (int sno : snos) {
            String holder = holds.get(seatKey(gno, sno));
            if (holder == null || !holder.equals(String.valueOf(mno))) {
                failReason.append(sno).append(" not held by you; ");
                return false;
            }
        }

        for (int sno : snos) {
            sold.add(sno);
            holds.remove(seatKey(gno, sno));
            userHoldSet(mno, gno).remove(sno);
            try { seatLock(gno, sno).forceUnlock(); } catch (Exception ignore) {}
        }

        // ✅ Redis 카운터 추가/갱신
        incrementUserBookedCount(mno, gno, snos.size());

        persistReservationsOrThrow(mno, gno, snos);
        return true;

    }   // func end

    private void incrementUserBookedCount(int mno, int gno, int addCount) {
        String key = "user_booking_count:" + mno + ":" + gno;
        RAtomicLong counter = redisson.getAtomicLong(key);
        counter.addAndGet(addCount);
        counter.expire(7, TimeUnit.DAYS); // TTL은 예매 TTL과 동일하게 6시간
    }

    // 프론트에서 잔여석 표시 기능
    public int remainingSelectableSeats(int mno, int gno) {
        int confirmed = (int) redisson.getAtomicLong("user_booking_count:" + mno + ":" + gno).get();
        int holds = userHoldSet(mno, gno).size();
        return Math.max(0, MAX_SEATS_PER_USER - (confirmed + holds));
    }

    // DB에 reservations insert (트랜잭션)
    @Transactional(rollbackFor = Exception.class)
    public void persistReservationsOrThrow(int mno, int gno, List<Integer> snos) {
        if (snos == null || snos.isEmpty()) {
            throw new IllegalArgumentException("snos is empty");
        }

        for (int sno : snos) {
            ReservationsDto dto = new ReservationsDto();
            dto.setMno(mno);
            dto.setSno(sno);
            dto.setGno(gno);
            dto.setStatus("reserved");

            if(!seatsMapper.insertReservation(dto)) throw new IllegalStateException("예약테이블 insert 오류"); // rno가 DTO 안에 자동 주입됨
            int rno = dto.getRno();

            boolean result = ticketsService.ticketWrite(rno);
            if (!result) {
                throw new IllegalStateException("Failed to insert reservation (mno=" + mno +
                        ", gno=" + gno + ", sno=" + sno + ")");
            }   // if end
        }   // for end
    }   // func end

    // 예매 취소시 로직
    // SeatLockService.java (추가)
    public void onReservationCancelled(int mno, int gno, int sno) {
        try {
            // 1) SOLD 해제
            soldSet(gno).remove(sno);

            // 2) 유저 예매 카운터 디크리먼트 (하한 0)
            decrementUserBookedCount(mno, gno, 1);

            // 3) 안전 차원에서 락도 강제 해제(있다면)
            try { seatLock(gno, sno).forceUnlock(); } catch (Exception ignore) {}
        } catch (Exception e) {
            System.out.println("[SeatLockService] onReservationCancelled error: " + e.getMessage());
        }
    }

    private void decrementUserBookedCount(int mno, int gno, int dec) {
        String key = "user_booking_count:" + mno + ":" + gno;
        RAtomicLong counter = redisson.getAtomicLong(key);
        // CAS 루프 (0 미만 방지)
        while (true) {
            long cur = counter.get();
            if (cur <= 0) break;
            long next = Math.max(0, cur - dec);
            if (counter.compareAndSet(cur, next)) break;
        }
        // TTL 갱신(필요 시 프로젝트 규칙에 맞춰 조정: 예시 7일)
        counter.expire(7, TimeUnit.DAYS);
    }

    // =========================
    // (부분) 상태 조회
    // =========================
    /**
     * 좌석 상태 조회(프론트 비활성화용)
     * - "BLOCKED" : 시니어 전용석이지만 일반예매 D-2 이전이라 비공개
     * - "SOLD" / "HELD" / "HELD_BY_ME" / "AVAILABLE" / "INVALID"
     */
    public Map<Integer, String> getSeatStatusFor(int gno, int mno, List<Integer> snos) {
        Map<Integer, String> res = new LinkedHashMap<>();
        if (snos == null || snos.isEmpty()) return res;

        boolean seniorOpen = isSeniorOpenForGeneral(gno);
        RSet<Integer> sold = soldSet(gno);
        RMapCache<String, String> holds = holdMap();

        for (int sno : snos) {
            // 1) 무결성
            if (!seatCsvService.existsSeatBySno(sno)) { res.put(sno, "INVALID"); continue; }

            // 2) 시니어석 블록 표시
            if (seatCsvService.isSeniorSeat(sno) && !seniorOpen) {
                res.put(sno, "BLOCKED");
                continue;
            }

            // 3) SOLD/HELD/AVAILABLE
            if (sold.contains(sno)) { res.put(sno, "SOLD"); continue; }
            String holder = holds.get(seatKey(gno, sno));
            if (holder != null) {
                res.put(sno, holder.equals(String.valueOf(mno)) ? "HELD_BY_ME" : "HELD");
            } else {
                res.put(sno, "AVAILABLE");
            }
        }
        return res;
    }

    // =========================
    // 고아 hold 자동 청소 (변경 없음)
    // =========================
    @Scheduled(fixedDelay = 2000)
    public void cleanupExpiredSeatHolds() {
        try {
            var map = holdMap();
            for (Map.Entry<String, String> e : map.entrySet()) {
                String key = e.getKey();   // "gno:sno"
                String mnoStr = e.getValue();

                int idx = key.indexOf(':');
                if (idx <= 0 || idx >= key.length() - 1) {
                    map.remove(key);
                    continue;
                }
                int gno = Integer.parseInt(key.substring(0, idx));
                int sno = Integer.parseInt(key.substring(idx + 1));
                int mno = Integer.parseInt(mnoStr);

                boolean alive =
                        redisson.getBucket(RedisKeys.keySession(gno, mno)).isExists()
                                || redisson.getBucket(RedisKeys.SESSION_PREFIX + mno).isExists();

                if (!alive) {
                    map.remove(key);
                    userHoldSet(mno, gno).remove(sno);
                    try { seatLock(gno, sno).forceUnlock(); } catch (Exception ignore) {}
                }
            }
        } catch (Exception ex) {
            System.out.println("[SeatLockService] cleanup error: " + ex.getMessage());
        }
    }
}
