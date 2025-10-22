package phoenix.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import phoenix.model.mapper.SeatsMapper;
import phoenix.util.RedisKeys;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 좌석 락/보류/확정까지 전부 처리하는 핵심 서비스
 * - 좌석 식별은 sno(PK)로 통일
 * - 모든 존의 좌석 수/패턴이 동일 → 프론트는 공용 그리드 템플릿 사용 + 부분 상태만 조회
 *
 * Redis 키:
 *  - SEAT_HOLD_MAP (RMapCache<String,String>): "gno:sno" -> "mno" (TTL 지원)
 *  - SEAT_SOLD_SET:<gno> (RSet<Integer>): sold snos
 *  - user:hold:<mno>:<gno> (RSetCache<Integer>): 유저의 임시 보유 sno 목록(TTL)
 *  - gate:{gno}:session:{mno} (RBucket<?>): 게이트 세션 alive 여부(키 존재)  ← ✅ 신규(스코프)
 *  - session:{mno}              (RBucket<?>): 레거시 전역 세션 키                ← (fallback)
 *  - user_booking:<mno>:<gno>   (RBucket<Boolean>): 공연 단위 결제 완료 플래그(중복 방지)
 */
@Service
@EnableScheduling
@RequiredArgsConstructor
public class SeatLockService {

    private final SeatsMapper seatsMapper;
    private final RedissonClient redisson;

    /** 임시 보류 TTL (초) — 2분 */
    private static final long HOLD_TTL_SECONDS = 120;
    /** 1인당 동시 보유 가능 좌석 수 */
    private static final int MAX_SEATS_PER_USER = 4;

    /* =========================
     * Redis Accessors
     * ========================= */
    /** 좌석 임시 보류 맵(키 TTL 지원) — key:"gno:sno", val:"mno" */
    private RMapCache<String, String> holdMap() {
        return redisson.getMapCache(RedisKeys.SEAT_HOLD_MAP);
    }
    /** gno별 매진 좌석 세트 — 원소:sno */
    private RSet<Integer> soldSet(int gno) {
        return redisson.getSet(RedisKeys.SEAT_SOLD_SET + ":" + gno);
    }
    /** 유저별 임시 보유 좌석 세트 — 원소:sno, TTL 지원 */
    private RSetCache<Integer> userHoldSet(int mno, int gno) {
        return redisson.getSetCache("user:hold:" + mno + ":" + gno);
    }
    /** 좌석 분산락 키/객체 */
    private String seatKey(int gno, int sno) { return gno + ":" + sno; }
    private RLock seatLock(int gno, int sno) { return redisson.getLock("seat:lock:" + seatKey(gno, sno)); }

    /* =========================
     * Gate / Duplicate guards
     * ========================= */
    /**
     * 게이트 세션 생존 여부
     * - GateService는 gno 스코프 키(gate:{gno}:session:{mno})를 사용
     * - 레거시 전역 키(session:{mno})도 fallback 으로 허용(점진 이행)
     */
    private boolean hasActiveSession(int mno, int gno) {
        boolean scoped = redisson.getBucket(RedisKeys.keySession(gno, mno)).isExists();     // gate:{gno}:session:{mno}
        boolean legacy = redisson.getBucket(RedisKeys.SESSION_PREFIX + mno).isExists();     // session:{mno}
        return scoped || legacy;
    }

    /** 공연 단위 중복 예매 여부 */
    private boolean hasUserAlreadyBooked(int mno, int gno) {
        RBucket<Boolean> b = redisson.getBucket("user_booking:" + mno + ":" + gno);
        return Boolean.TRUE.equals(b.get());
    }

    /** 예매 완료 플래그 세팅 (중복 방지) */
    private void markUserAsBooked(int mno, int gno) {
        redisson.getBucket("user_booking:" + mno + ":" + gno).set(true, 6, TimeUnit.HOURS);
    }

    /* =========================
     * SOLD 세트 복구 (서버 기동 시)
     * ========================= */
    @PostConstruct
    public void initSoldFromDb() {
        try {
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
            System.out.println("[SeatLockService] SOLD 복구 완료 (sets=" + sets + ", seats=" + seats + ")");
        } catch (Exception e) {
            System.out.println("[SeatLockService] SOLD 복구 실패: " + e.getMessage());
        }
    }

    /* =========================
     * 좌석 선택 (락 시도)
     * ========================= */
    /**
     * 좌석 선택 시도 (락 → holdMap/userHoldSet 기록)
     * @return 코드
     *   1  : OK
     *  -1  : 세션 없음
     *  -2  : 이미 해당 gno 결제완료
     *  -3  : 매진이거나 락 선점 실패(동시성)
     *  -4  : 한도(4개) 초과
     *  -5  : INVALID_SEAT (sno가 zno에 속하지 않음 등)
     */
    public int tryLockSeat(int mno, int gno, int zno, int sno) throws InterruptedException {
        // ✅ gno 스코프 세션 체크 (레거시도 fallback)
        if (!hasActiveSession(mno, gno)) return -1;
        if (hasUserAlreadyBooked(mno, gno)) return -2;

        // 유효성 검증: sno ∈ zno (클라 변조 방지)
        if (!seatsMapper.existsSeatInZone(zno, sno)) return -5;

        // 매진 여부
        if (soldSet(gno).contains(sno)) return -3;

        // 한도 확인
        RSetCache<Integer> myHolds = userHoldSet(mno, gno);
        if (myHolds.size() >= MAX_SEATS_PER_USER) return -4;

        // 분산락 즉시 시도(대기 0, 임대 120초)
        RLock lock = seatLock(gno, sno);
        boolean acquired = lock.tryLock(0, HOLD_TTL_SECONDS, TimeUnit.SECONDS);
        if (!acquired) return -3;

        // 임시 보류 기록
        holdMap().put(seatKey(gno, sno), String.valueOf(mno), HOLD_TTL_SECONDS, TimeUnit.SECONDS);
        myHolds.add(sno, HOLD_TTL_SECONDS, TimeUnit.SECONDS);

        return 1;
    }

    /* =========================
     * 좌석 해제
     * ========================= */
    /**
     * 내 임시 좌석 해제
     * @return true: 성공, false: 권한 없음/유효하지 않음
     */
    public boolean releaseSeat(int mno, int gno, int zno, int sno) {
        // 유효성 검증 실패 시 바로 종료
        if (!seatsMapper.existsSeatInZone(zno, sno)) return false;

        String key = seatKey(gno, sno);
        String holder = holdMap().get(key);

        // 내가 보유자가 아니면 해제 불가
        if (holder == null || !holder.equals(String.valueOf(mno))) return false;

        // holdMap / 내 보유세트 제거
        holdMap().remove(key);
        userHoldSet(mno, gno).remove(sno);

        // 좌석락 즉시 해제
        try { seatLock(gno, sno).forceUnlock(); } catch (Exception ignore) {}

        return true;
    }

    /* =========================
     * 결제 확정
     * ========================= */
    public boolean confirmSeats(int mno, int gno, List<Integer> snos, StringBuilder failReason) {
        // ✅ gno 스코프 세션 체크 (레거시도 fallback)
        if (!hasActiveSession(mno, gno)) { failReason.append("no session"); return false; }
        if (hasUserAlreadyBooked(mno, gno)) { failReason.append("already booked"); return false; }
        if (snos == null || snos.isEmpty()) { failReason.append("empty"); return false; }

        // 검증 1: SOLD 아님
        RSet<Integer> sold = soldSet(gno);
        for (int sno : snos) {
            if (sold.contains(sno)) { failReason.append(sno).append(" sold; "); return false; }
        }
        // 검증 2: HOLD_BY_ME
        RMapCache<String, String> holds = holdMap();
        for (int sno : snos) {
            String holder = holds.get(seatKey(gno, sno));
            if (holder == null || !holder.equals(String.valueOf(mno))) {
                failReason.append(sno).append(" not held by you; ");
                return false;
            }
        }

        // 커밋: SOLD 추가, hold/보유세트/락 정리
        for (int sno : snos) {
            sold.add(sno);
            holds.remove(seatKey(gno, sno));
            userHoldSet(mno, gno).remove(sno);
            try { seatLock(gno, sno).forceUnlock(); } catch (Exception ignore) {}
        }
        markUserAsBooked(mno, gno);
        return true;
    }

    /* =========================
     * (부분) 상태 조회
     * ========================= */
    public Map<Integer, String> getSeatStatusFor(int gno, int mno, List<Integer> snos) {
        Map<Integer, String> res = new LinkedHashMap<>();
        if (snos == null || snos.isEmpty()) return res;

        RSet<Integer> sold = soldSet(gno);
        RMapCache<String, String> holds = holdMap();

        for (int sno : snos) {
            // 최소 무결성: sno 존재 여부
            if (!seatsMapper.existsSeatBySno(sno)) { res.put(sno, "INVALID"); continue; }

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

    /* =========================
     * 고아 hold 자동 청소
     * ========================= */
    @Scheduled(fixedDelay = 2000)
    public void cleanupExpiredSeatHolds() {
        try {
            var map = holdMap();
            for (Map.Entry<String, String> e : map.entrySet()) {
                String key = e.getKey();   // "gno:sno"
                String mnoStr = e.getValue();

                // key 파싱
                int idx = key.indexOf(':');
                if (idx <= 0 || idx >= key.length() - 1) {
                    map.remove(key); // 포맷 비정상 → 안전 제거
                    continue;
                }
                int gno = Integer.parseInt(key.substring(0, idx));
                int sno = Integer.parseInt(key.substring(idx + 1));
                int mno = Integer.parseInt(mnoStr);

                // ✅ gno 스코프 세션 + 레거시 전역 키 둘 다 체크
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
