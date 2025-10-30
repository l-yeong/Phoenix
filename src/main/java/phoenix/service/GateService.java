package phoenix.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import phoenix.util.RedisKeys;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * =============================================================
 * 🧩 [테스트용 GateService]
 *  - gno(경기) 단위로 완전히 분리된 구조
 *  - 세마포어, 대기열, 활성유저, 세션 모두 gno별로 분리
 *  - 로그를 매우 풍부하게 추가하여 상태 확인 가능
 * =============================================================
 */
@Service
@RequiredArgsConstructor
@EnableScheduling
public class GateService {

    private final GameService gameService;
    private final RedissonClient redisson;

    private static final int MAX_PERMITS = 1; // 동시 입장 허용 인원
    private static final long SESSION_MINUTES = 5; // 세션 TTL (분)

    // ===== Redis Accessors =====
    private RSemaphore semaphore(int gno) { return redisson.getSemaphore(RedisKeys.keySemaphore(gno)); }
    private RBlockingQueue<Integer> queue(int gno) { return redisson.getBlockingQueue(RedisKeys.keyQueue(gno)); }
    private RSet<Integer> activeSet(int gno) { return redisson.getSet(RedisKeys.keyActiveSet(gno)); }
    private RSet<Integer> waitingSet(int gno) { return redisson.getSet(RedisKeys.keyWaitingSet(gno)); }
    private RBucket<String> sessionBucket(int gno, int mno){ return redisson.getBucket(RedisKeys.keySession(gno, mno)); }
    private RSet<Integer> gnoIndex(){ return redisson.getSet(RedisKeys.GATE_GNO_INDEX); }

    @PostConstruct
    public void clearAllGateDataOnStartup() {
        RKeys keys = redisson.getKeys();
        long deleted = 0;

        // "gate:"로 시작하는 모든 키를 전부 삭제
        Iterable<String> all = keys.getKeysByPattern("gate:*");
        for (String key : all) {
            keys.delete(key);
            deleted++;
        }


    }

    private void ensureSemaphoreInitialized(int gno) {
        gnoIndex().add(gno); // 🔴 추가: 스케줄러가 이 gno를 순회할 수 있게 등록
        RSemaphore sem = redisson.getSemaphore(String.format("gate:%d:semaphore", gno));
        RBucket<Boolean> boostedFlag = redisson.getBucket(String.format("gate:%d:boosted", gno));

        // ✅ 세마포어가 없으면 완전 신규 생성 (최초 공연 등록 시)
        if (!sem.isExists()) {
            sem.trySetPermits(MAX_PERMITS);
            boostedFlag.set(true);
            return;
        }

        // ✅ 세마포어가 0이고 아직 보정 안 했을 때만 +1
        int available = sem.availablePermits();
        if (available == 0 && !Boolean.TRUE.equals(boostedFlag.get())) {
            sem.release(1);
            boostedFlag.set(true);
            System.out.printf("🔧 [GateService] gno=%d 세마포어 보정 완료 (0 → 1)%n", gno);
        } else {
            System.out.printf("✅ [GateService] gno=%d 세마포어 정상 상태 (permits=%d, boosted=%s)%n",
                    gno, available, boostedFlag.get());
        }
    }


    // ============ Public APIs ============

    /** 🟢 대기열 등록 */
    public EnqueueResult enqueue(int mno, int gno) {
        ensureSemaphoreInitialized(gno);

        // 🔴 추가: 내 세션이 없는데 activeSet에는 남아있으면 stale → 정리 후 퍼밋 반환
        if (!sessionBucket(gno, mno).isExists() && activeSet(gno).remove(mno)) {
            try { semaphore(gno).release(); } catch (Exception ignore) {}
            System.out.println("🧹 [enqueue] stale active 제거 및 퍼밋 반환 (mno=" + mno + ", gno=" + gno + ")");
        }

        if (!gameService.isReservable(gno)) {
            System.out.println(" 🚫 예약 불가 경기입니다.");
            return new EnqueueResult(false, 0);
        }

        if (!waitingSet(gno).add(mno)) {
            System.out.println(" ⚠ 이미 대기열에 있는 사용자입니다.");
            return new EnqueueResult(true, queue(gno).size());
        }

        queue(gno).add(mno);

        assignNextIfPossible(gno);
        return new EnqueueResult(true, queue(gno).size());
    }

    // gate 남은 세션 ttl 보기
    public long remainTtlMillis(int mno, int gno) {
        RBucket<String> b = sessionBucket(gno, mno);
        try {
            long ms = b.remainTimeToLive(); // Redisson: 남은 TTL(ms), TTL 없으면 -1
            return Math.max(0L, ms);
        } catch (Exception e) {
            return 0L;
        }
    }

    /** 🚪 세마포어 여유가 있으면 다음 대기자 입장 */
    public void assignNextIfPossible(int gno) {

        try {
            int permits = semaphore(gno).availablePermits();

            if (permits <= 0) {
                System.out.println(" ❌ 퍼밋 없음 → 대기 유지");
                return;
            }

            Integer nextUser = queue(gno).poll();
            if (nextUser == null) {
                System.out.println(" ⚠ 대기자 없음 → 종료");
                return;
            }
            if (activeSet(gno).contains(nextUser)) {
                System.out.println(" ⚠ 이미 활성 상태 사용자(" + nextUser + ")");
                return;
            }

            boolean acquired = semaphore(gno).tryAcquire();
            if (!acquired) {
                System.out.println(" ❌ tryAcquire 실패 → 대기열 뒤로 보냄");
                queue(gno).add(nextUser);
                return;
            }

            // 세션 부여
            sessionBucket(gno, nextUser).set("alive", SESSION_MINUTES, TimeUnit.MINUTES);
            activeSet(gno).add(nextUser);
            waitingSet(gno).remove(nextUser);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 🔍 세션 alive 확인 */
    public boolean isEntered(int mno, int gno) {
        boolean ok = sessionBucket(gno, mno).isExists();
        return ok;
    }

    // 퇴장
    public boolean leave(int mno, int gno) {
        // 세션 제거
        sessionBucket(gno, mno).delete();

        // active/queue/waiting 모두 제거
        boolean wasActive  = activeSet(gno).remove(mno);
        boolean wasQueued1 = waitingSet(gno).remove(mno);
        boolean wasQueued2 = queue(gno).remove(mno); // RBlockingQueue도 remove 지원

        if (wasActive) {
            try {
                semaphore(gno).release();
                System.out.println(" 🔄 퍼밋 반환됨 → 남은 퍼밋=" + semaphore(gno).availablePermits());
            } catch (Exception ignore) {}
        }

        // 다음 사람 입장 시도
        assignNextIfPossible(gno);

        return wasActive || wasQueued1 || wasQueued2;
    }

    /** 📊 대기열 길이 */
    public int waitingCount(int gno) {

        return queue(gno).size();
    }

    /** 🧮 남은 퍼밋 */
    public int availablePermits(int gno) {

        return semaphore(gno).availablePermits();
    }


    /** 📍 내 순번 조회 */
    public Integer positionOf(int mno, int gno) {
        if (isEntered(mno, gno)) return 0;

        RBlockingQueue<Integer> q = queue(gno);
        int idx = 1;
        for (Integer uid : q) {
            if (uid.equals(mno)) return idx;
            idx++;
        }
        return null;
    }

    // ============ 스케줄러 ============
    @Scheduled(fixedDelay = 2000)
    public void reapExpiredSessions() {
        try {
            Set<Integer> shows = gnoIndex().readAll();
            for (Integer gno : shows) {
                RSet<Integer> actives = activeSet(gno);
                for (Integer mno : actives) {
                    boolean alive = sessionBucket(gno, mno).isExists();
                    if (!alive) {
                        actives.remove(mno);
                        try { semaphore(gno).release(); } catch (Exception ignore) {}
                        assignNextIfPossible(gno);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 내부 결과 DTO
    public record EnqueueResult(boolean queued, int waiting) {}
}
