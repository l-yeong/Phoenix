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

    private final SeatsMapper seatsMapper;
    private final RedissonClient redisson;
    private final SeatCsvService seatCsvService;
    private final GameService gameService;
    private final TicketsService ticketsService;

    private static final long HOLD_TTL_SECONDS = 120;
    private static final int  MAX_SEATS_PER_USER = 4;
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    // ===== Redis Accessors =====
    private RMapCache<String, String> holdMap() { return redisson.getMapCache(RedisKeys.SEAT_HOLD_MAP); }
    private RSet<Integer> soldSet(int gno)       { return redisson.getSet(RedisKeys.SEAT_SOLD_SET + ":" + gno); }
    private RSetCache<Integer> userHoldSet(int mno, int gno) { return redisson.getSetCache("user:hold:" + mno + ":" + gno); }
    private String seatKey(int gno, int sno)     { return gno + ":" + sno; }
    private RLock seatLock(int gno, int sno)     { return redisson.getLock("seat:lock:" + seatKey(gno, sno)); }

    // ===== Session guard (scoped only) =====
    private boolean hasActiveSession(int mno, int gno) {
        return redisson.getBucket(RedisKeys.keySession(gno, mno)).isExists();
    }

    // ===== D-2 senior open for GENERAL =====
    private boolean isSeniorOpenForGeneral(int gno) {
        var game = gameService.findByGno(gno);
        if (game == null || game.getDate() == null || game.getTime() == null) return false;
        ZonedDateTime startAt = ZonedDateTime.of(game.getDate(), game.getTime(), ZONE_SEOUL);
        ZonedDateTime gate = startAt.minusDays(2);
        return !ZonedDateTime.now(ZONE_SEOUL).isBefore(gate); // now >= start-2d
    }

    @PostConstruct
    public void initSoldFromDb() {
        try {
            // 1) SOLD 복구 (채널 무관)
            List<Integer> gnos = seatsMapper.findAllGnosHavingReserved();
            if (gnos != null && !gnos.isEmpty()) {
                int sets = 0, seats = 0;
                for (int gno : gnos) {
                    List<Integer> snos = seatsMapper.findReservedSnosByGno(gno);
                    RSet<Integer> sold = soldSet(gno);
                    if (sold.isEmpty() && snos != null && !snos.isEmpty()) {
                        sold.addAll(snos);
                        sets++; seats += snos.size();
                    }
                }
                System.out.printf("[SeatLockService] SOLD 복구 완료 (sets=%d, seats=%d)%n", sets, seats);
            } else {
                System.out.println("[SeatLockService] SOLD 복구: reserved 데이터 없음");
            }

            // 2) 일반 예매 카운터 복구 (channel='general'만)
            List<ReservationsDto> generalSummary = seatsMapper.findUserReservedCountSummaryGeneral();
            int restoredUsers = 0;
            for (ReservationsDto dto : generalSummary) {
                String key = "user_booking_count:" + dto.getMno() + ":" + dto.getGno();
                RAtomicLong counter = redisson.getAtomicLong(key);
                counter.set(dto.getCount());
                counter.expire(7, TimeUnit.DAYS);
                restoredUsers++;
            }
            System.out.printf("[SeatLockService] 일반 카운터 복구 (users=%d)%n", restoredUsers);

        } catch (Exception e) {
            System.out.println("[SeatLockService] 복구 실패: " + e.getMessage());
        }
    }

    // ===== Lock (GENERAL flow) =====
    /**
     * @return 1:OK, -1:no session, -3:sold/lock fail, -4:limit(4), -5:invalid seat, -6:senior not open
     */
    public int tryLockSeat(int mno, int gno, int zno, int sno) throws InterruptedException {
        if (!hasActiveSession(mno, gno)) return -1;
        if (!seatCsvService.existsSeatInZone(zno, sno)) return -5;
        if (seatCsvService.isSeniorSeat(sno) && !isSeniorOpenForGeneral(gno)) return -6;
        if (soldSet(gno).contains(sno)) return -3;

        RSetCache<Integer> myHolds = userHoldSet(mno, gno);
        int holdCount = myHolds.size();

        RAtomicLong bookedCount = redisson.getAtomicLong("user_booking_count:" + mno + ":" + gno);
        int confirmedCount = (int) bookedCount.get();

        if (confirmedCount + holdCount >= MAX_SEATS_PER_USER) return -4;

        RLock lock = seatLock(gno, sno);
        if (!lock.tryLock(0, HOLD_TTL_SECONDS, TimeUnit.SECONDS)) return -3;

        holdMap().put(seatKey(gno, sno), String.valueOf(mno), HOLD_TTL_SECONDS, TimeUnit.SECONDS);
        myHolds.add(sno, HOLD_TTL_SECONDS, TimeUnit.SECONDS);
        return 1;
    }

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

        // GENERAL channel
        incrementUserBookedCount(mno, gno, snos.size());
        persistReservationsOrThrow(mno, gno, snos, "general");
        return true;
    }

    // ===== Status for UI =====
    public Map<Integer, String> getSeatStatusFor(int gno, int mno, List<Integer> snos) {
        Map<Integer, String> res = new LinkedHashMap<>();
        if (snos == null || snos.isEmpty()) return res;

        boolean seniorOpen = isSeniorOpenForGeneral(gno);
        RSet<Integer> sold = soldSet(gno);
        RMapCache<String, String> holds = holdMap();

        for (int sno : snos) {
            if (!seatCsvService.existsSeatBySno(sno)) { res.put(sno, "INVALID"); continue; }

            if (sold.contains(sno)) { res.put(sno, "SOLD"); continue; }
            if (seatCsvService.isSeniorSeat(sno) && !seniorOpen) { res.put(sno, "BLOCKED"); continue; }

            String holder = holds.get(seatKey(gno, sno));
            if (holder != null) res.put(sno, holder.equals(String.valueOf(mno)) ? "HELD_BY_ME" : "HELD");
            else res.put(sno, "AVAILABLE");
        }
        return res;
    }

    public int remainingSelectableSeats(int mno, int gno) {
        int confirmed = (int) redisson.getAtomicLong("user_booking_count:" + mno + ":" + gno).get();
        int holds = userHoldSet(mno, gno).size();
        return Math.max(0, MAX_SEATS_PER_USER - (confirmed + holds));
    }

    private void incrementUserBookedCount(int mno, int gno, int addCount) {
        String key = "user_booking_count:" + mno + ":" + gno;
        RAtomicLong counter = redisson.getAtomicLong(key);
        counter.addAndGet(addCount);
        counter.expire(7, TimeUnit.DAYS);
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistReservationsOrThrow(int mno, int gno, List<Integer> snos, String channel) {
        if (snos == null || snos.isEmpty()) {
            throw new IllegalArgumentException("snos is empty");
        }
        for (int sno : snos) {
            ReservationsDto dto = new ReservationsDto();
            dto.setMno(mno);
            dto.setSno(sno);
            dto.setGno(gno);
            dto.setStatus("reserved");
            dto.setChannel(channel); // "general" or "senior"
            if (!seatsMapper.insertReservationWithChannel(dto)) {
                throw new IllegalStateException("예약테이블 insert 오류");
            }
            int rno = dto.getRno();
            if (!ticketsService.ticketWrite(rno)) {
                throw new IllegalStateException("Failed to issue ticket");
            }
        }
    }

    public void onReservationCancelled(int mno, int gno, int sno, String channel) {
        try {
            soldSet(gno).remove(sno);
            if ("senior".equalsIgnoreCase(channel)) {
                decrementSeniorBookedCount(mno, gno, 1);
            } else {
                decrementUserBookedCount(mno, gno, 1);
            }
            try { seatLock(gno, sno).forceUnlock(); } catch (Exception ignore) {}
        } catch (Exception e) {
            System.out.println("[SeatLockService] onReservationCancelled error: " + e.getMessage());
        }
    }

    private void decrementUserBookedCount(int mno, int gno, int dec) {
        String key = "user_booking_count:" + mno + ":" + gno;
        RAtomicLong counter = redisson.getAtomicLong(key);
        while (true) {
            long cur = counter.get();
            if (cur <= 0) break;
            long next = Math.max(0, cur - dec);
            if (counter.compareAndSet(cur, next)) break;
        }
        counter.expire(7, TimeUnit.DAYS);
    }

    private void decrementSeniorBookedCount(int mno, int gno, int dec) {
        String key = RedisKeys.keySeniorBooked(mno, gno);
        RAtomicLong counter = redisson.getAtomicLong(key);
        while (true) {
            long cur = counter.get();
            if (cur <= 0) break;
            long next = Math.max(0, cur - dec);
            if (counter.compareAndSet(cur, next)) break;
        }
        counter.expire(7, TimeUnit.DAYS);
    }

    // ===== Orphan hold cleanup (keep this!) =====
    @Scheduled(fixedDelay = 2000)
    public void cleanupExpiredSeatHolds() {
        try {
            var map = holdMap(); // key: "gno:sno", val: "mno"
            for (Map.Entry<String, String> e : map.entrySet()) {
                String key = e.getKey();
                String mnoStr = e.getValue();

                int idx = key.indexOf(':');
                if (idx <= 0 || idx >= key.length() - 1) {
                    map.remove(key);
                    continue;
                }
                int gno = Integer.parseInt(key.substring(0, idx));
                int sno = Integer.parseInt(key.substring(idx + 1));
                int mno = Integer.parseInt(mnoStr);

                boolean alive = hasActiveSession(mno, gno);
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

    // ===== Snapshot for /seat/held & /seat/confirm/all =====
    public Set<Integer> getUserHoldSnapshot(int mno, int gno) {
        RSetCache<Integer> set = userHoldSet(mno, gno);
        // 읽기 전용 스냅샷으로 반환 (iteration 안전)
        Set<Integer> res = new LinkedHashSet<>();
        try {
            // Redisson 3.x: readAll() 지원 — 내부 네트워크 왕복 1회로 모두 가져옴
            res.addAll(set.readAll());
        } catch (UnsupportedOperationException ignore) {
            // 혹시 구현체가 readAll 미지원이면 iterator로 수집
            for (Integer s : set) res.add(s);
        }
        return res;
    }
}
