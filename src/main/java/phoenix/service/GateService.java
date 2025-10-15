package phoenix.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import phoenix.util.RedisKeys;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * ===============================================================
 * [GateService]
 * 🎟️ 공연 예매 입장 제어(대기열 + 동시성 제어) 핵심 로직
 * ===============================================================
 *
 * ✅ 핵심 역할
 * 1. 동시에 입장 가능한 사용자 수를 제한 (예: 3명만)
 * 2. 나머지 인원은 "대기열"에 넣어 순서대로 입장시킴
 * 3. 입장 시 "1회용 토큰"을 발급하여 프론트에 전달
 * 4. 입장 중(게이트 통과)인 사용자는 TTL(세션 유지 시간) 동안만 예매 가능
 * 5. 이미 예매한 사용자는 대기열에 들어가지 못하도록 차단
 *
 * ⚙️ 기술 요소
 * - Redisson RSemaphore : 동시 입장 인원(퍼밋) 제어
 * - Redisson RBlockingQueue : 대기열(FIFO)
 * - Redisson RBucket : 세션 상태(userId), 토큰 저장(token → userId)
 * - Redisson RSet : 현재 활성 사용자 목록 관리
 *
 * 🧱 TTL 설정
 * - 게이트 세션 유지시간 : 5분
 * - 1회용 토큰 유효시간 : 30초
 * - RSemaphore 퍼밋 수 : 3명 (동시 입장 제한)
 */
@Service
@RequiredArgsConstructor
@EnableScheduling // @Scheduled로 세션 만료 회수를 주기적으로 수행
public class GateService {

    private final RedissonClient redisson;

    /** 동시 입장 허용 인원 (3명까지만 동시에 예매 페이지 접근 가능) */
    private static final int MAX_PERMITS = 3;

    /** 게이트 세션 TTL (5분) — 입장 후 5분 동안만 좌석 선택 가능 */
    private static final long SESSION_MINUTES = 5;

    /** 입장 토큰 TTL (30초) — 토큰 발급 후 30초 안에 제출해야 함 */
    private static final long ADMISSION_TOKEN_TTL_SECONDS = 30;

    // ===============================================================
    // ✅ Redis 구조 접근자 (헬퍼)
    // ===============================================================

    /** [1] 동시 입장 제한 세마포어 */
    private RSemaphore semaphore() {
        // "gate:semaphore" 키에 연결된 Redisson 분산 세마포어
        // 퍼밋이 0이면 모든 입장이 꽉 찬 상태
        return redisson.getSemaphore(RedisKeys.GATE_SEMAPHORE);
    }

    /** [2] 대기열 큐 (FIFO: 먼저 들어온 사람이 먼저 입장) */
    private RBlockingQueue<String> queue() {
        return redisson.getBlockingQueue(RedisKeys.WAITING_QUEUE);
    }

    /** [3] 입장 토큰 발급 이벤트 송신용 토픽 (Pub/Sub 구조) */
    private RTopic admissionTopic() {
        return redisson.getTopic(RedisKeys.ADMISSION_TOPIC);
    }

    /** [4] 현재 게이트 안에 있는 사용자 Set */
    private RSet<String> activeSet() {
        return redisson.getSet(RedisKeys.ACTIVE_SET);
    }

    // ===============================================================
    // ⚙️ 초기화 로직 (앱 시작 시 3개의 퍼밋 세팅)
    // ===============================================================
    @PostConstruct
    public void init() {
        // Redis 상에 퍼밋 개수를 지정 (이미 존재해도 무해)
        semaphore().trySetPermits(MAX_PERMITS);
    }

    // ===============================================================
    // 🧩 [1] 중복 예매 확인 로직
    // ===============================================================

    /**
     * [hasUserAlreadyBooked]
     * - 이미 해당 공연(showId)을 예매 완료한 사용자인지 확인
     * - 예매 완료 시 SeatLockService에서 다음 키로 기록됨:
     *      user_booking:{userId}:{showId} = true
     * - Redis의 Boolean 값을 읽어서 true면 이미 예매함.
     */
    private boolean hasUserAlreadyBooked(String userId, String showId) {
        RBucket<Boolean> b = redisson.getBucket("user_booking:" + userId + ":" + showId);
        return Boolean.TRUE.equals(b.get());
    }

    // ===============================================================
    // 🚪 [2] 대기열 등록 (enqueue)
    // ===============================================================

    /**
     * [enqueue]
     * - 사용자가 "예매 페이지 입장 시도"를 하면 호출됨
     * - ① 이미 예매한 사용자라면 대기열에 넣지 않음
     * - ② 아직 예매하지 않았다면 대기열에 추가
     * - ③ 빈 자리가 있으면 assignNextIfPossible()로 토큰 발급 시도
     *
     * @param userId 현재 사용자의 ID
     * @param showId 공연(혹은 회차) ID
     * @return EnqueueResult(queued 여부, 현재 대기열 인원수)
     */
    public EnqueueResult enqueue(String userId, String showId) {
        // 1️⃣ 이미 예매한 사용자인 경우 즉시 차단 (대기열 점유 방지)
        if (hasUserAlreadyBooked(userId, showId)) {
            return new EnqueueResult(false, 0);
        }

        // 2️⃣ 대기열(FIFO 큐)에 사용자 추가
        queue().add(userId);

        // 3️⃣ 빈 슬롯이 있다면 즉시 다음 사람을 입장시킴
        assignNextIfPossible();

        // 현재 대기열 길이 반환
        return new EnqueueResult(true, queue().size());
    }

    // ===============================================================
    // 🎫 [3] 다음 대기자에게 입장 토큰 발급
    // ===============================================================

    /**
     * [assignNextIfPossible]
     * - 세마포어 퍼밋이 남아있을 때 호출됨
     * - 대기열의 맨 앞 사용자를 꺼내서
     *   입장 토큰(admission token)을 발급한다.
     * - 발급된 토큰은 30초 동안만 유효하며,
     *   이 시간 안에 프론트에서 "/enter" 요청을 보내야 입장 확정됨.
     */
    public void assignNextIfPossible() {
        try {
            // 1️⃣ 퍼밋이 남아 있지 않으면 (입장 인원 꽉 참)
            if (semaphore().availablePermits() <= 0) return;

            // 2️⃣ 대기열에서 맨 앞 유저를 꺼냄
            String nextUser = queue().poll();
            if (nextUser == null) return; // 대기열 비었으면 종료

            // 3️⃣ 퍼밋 1개 확보 시도 (tryAcquire: 즉시 반환)
            boolean acquired = semaphore().tryAcquire();
            if (!acquired) {
                // 경쟁상황으로 실패 시 다시 큐에 되돌림
                queue().add(nextUser);
                return;
            }

            // 4️⃣ 1회용 토큰 생성 (UUID)
            String token = UUID.randomUUID().toString();

            // 5️⃣ Redis에 토큰 → userId 저장 (30초 TTL)
            redisson.getBucket(RedisKeys.ADMISSION_PREFIX + token)
                    .set(nextUser, ADMISSION_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

            // 6️⃣ 발급된 토큰 정보를 토픽(Pub/Sub)으로 전송
            //     실제 서비스에서는 SSE/WebSocket으로 사용자 개별 알림을 권장
            admissionTopic().publish(nextUser + "|" + token);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===============================================================
    // ✅ [4] 토큰 검증 및 입장 확정 (confirmEnter)
    // ===============================================================

    /**
     * [confirmEnter]
     * - 프론트에서 토큰을 제출하면 호출됨.
     * - ① 토큰이 유효하고 userId 일치 시 → 게이트 세션 시작
     * - ② 이미 예매한 사용자면 → 퍼밋을 즉시 반납하고 입장 거부
     * - ③ 세션은 Redis에 session:{userId} = alive (TTL=5분) 으로 저장됨.
     */
    public boolean confirmEnter(String userId, String token, String showId) {
        // 1️⃣ 토큰 → userId 매핑 확인
        RBucket<String> b = redisson.getBucket(RedisKeys.ADMISSION_PREFIX + token);
        String owner = b.get();

        // 2️⃣ 토큰이 존재하지 않거나 userId 불일치 시 입장 거부
        if (owner == null || !owner.equals(userId)) {
            try { semaphore().release(); } catch (Exception ignore) {}
            return false;
        }

        // 3️⃣ 이미 예매 완료된 상태라면 입장 불가 (동시창 방지)
        if (hasUserAlreadyBooked(userId, showId)) {
            b.delete(); // 토큰 폐기
            try { semaphore().release(); } catch (Exception ignore) {}
            return false;
        }

        // 4️⃣ 토큰 삭제 (1회용)
        b.delete();

        // 5️⃣ 세션 생성: session:{userId} = "alive" (5분 TTL)
        redisson.getBucket(RedisKeys.SESSION_PREFIX + userId)
                .set("alive", SESSION_MINUTES, TimeUnit.MINUTES);

        // 6️⃣ 현재 활성 사용자 집합에 추가
        activeSet().add(userId);

        return true; // 입장 성공
    }

    // ===============================================================
    // 🚪 [5] 퇴장 처리 (수동)
    // ===============================================================

    /**
     * [leave]
     * - 사용자가 예매를 마치거나 직접 퇴장할 때 호출됨.
     * - ① session:{userId} 키 삭제
     * - ② activeSet 에서 제거
     * - ③ 세마포어 퍼밋 1개 반환 → 다음 대기자에게 기회 부여
     */
    public boolean leave(String userId) {
        // 세션 및 활성 사용자 제거
        redisson.getBucket(RedisKeys.SESSION_PREFIX + userId).delete();
        activeSet().remove(userId);

        // 퍼밋 반환 후 다음 사람 호출
        try { semaphore().release(); } catch (Exception ignore) {}
        assignNextIfPossible();

        return true;
    }

    // ===============================================================
    // 📊 [6] 상태 조회용 헬퍼
    // ===============================================================

    public int waitingCount() {
        // 현재 큐에 대기 중인 사람 수
        return queue().size();
    }

    public int availablePermits() {
        // 현재 남은 입장 가능 슬롯 수
        return semaphore().availablePermits();
    }

    // ===============================================================
    // 🕓 [7] 스케줄러: 세션 만료 회수
    // ===============================================================

    /**
     * [reapExpiredSessions]
     * - 2초마다 실행(@Scheduled)
     * - activeSet(현재 입장자 목록)을 순회하면서
     *   session:{userId} TTL이 만료된 사용자를 제거한다.
     * - 세션이 사라진 사용자는 자동으로 퍼밋이 반환되어
     *   다음 대기자가 입장 가능하게 된다.
     */
    @Scheduled(fixedDelay = 2000)
    public void reapExpiredSessions() {
        try {
            for (String uid : activeSet()) {
                // session:{userId} 키가 여전히 존재하는지 검사
                boolean alive = redisson.getBucket(RedisKeys.SESSION_PREFIX + uid).isExists();

                // 존재하지 않으면 TTL 만료된 것 → 회수 처리
                if (!alive) {
                    activeSet().remove(uid);
                    try { semaphore().release(); } catch (Exception ignore) {}
                    assignNextIfPossible(); // 다음 대기자 입장 시도
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===============================================================
    // 🧾 [8] 내부 결과 DTO(record)
    // ===============================================================

    /**
     * [EnqueueResult]
     * - enqueue() 요청의 결과를 표현하는 간단한 데이터 객체
     * - queued : 대기열 등록 성공 여부
     * - waiting : 현재 대기열 인원수
     */
    public record EnqueueResult(boolean queued, int waiting) {}
}
