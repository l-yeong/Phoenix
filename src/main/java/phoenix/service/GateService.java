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

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // 동시 입장 허용 인원 (3명까지만 동시에 예매 페이지 접근 가능)
    private static final int MAX_PERMITS = 3;

    // 게이트 세션 TTL (5분) — 입장 후 5분 동안만 좌석 선택 가능
    private static final long SESSION_MINUTES = 5;

    // 입장 토큰 TTL (30초) — 토큰 발급 후 30초 안에 제출해야 함
    private static final long ADMISSION_TOKEN_TTL_SECONDS = 30;

    // [SSE 추가] 유저별 Emitter 보관
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Redis 구조 접근자

    // 동시 입장 제한 세마포어
    // "gate:semaphore" 키에 연결된 Redisson 분산 세마포어
    // 퍼밋이 0이면 모든 입장이 꽉 찬 상태 ,,,  세마포어란 쉽게 말하면 쓰레드풀임.
    private RSemaphore semaphore() { return redisson.getSemaphore(RedisKeys.GATE_SEMAPHORE); }

    // 대기열 큐 => 대기자를 순차적으로 들여보냄 , 분산 대기열임.
    private RBlockingQueue<String> queue() { return redisson.getBlockingQueue(RedisKeys.WAITING_QUEUE); }

    // 현재 게이트 안에 있는 사용자 Set 집합
    private RSet<String> activeSet() {return redisson.getSet(RedisKeys.ACTIVE_SET);}


    // 초기화 로직 ==> 앱 스타트 시 3개의 퍼밋만 세팅
    // 스프링(Spring)에서 “빈(Bean)이 생성되고 DI(의존성 주입)가 끝난 직후 한 번만 자동 실행되는 초기화 어노테이션
    @PostConstruct
    public void init() {
        // Redis 상에 퍼밋 개수를 지정 (이미 존재해도 무해)
        semaphore().trySetPermits(MAX_PERMITS);
    }   // func end


    // 메소드 부분

    // 중복 예매 여부 확인 메소드
    // getBucket으로 참조주소값을 가져오고 그 값을 Bucket<V> 타입의 b에 저장한다.
    // 그것을 .get()해서 Redis 타입을 자동 파싱해서 자바 형식으로 가져와 true인지 false인지 확인한다.
    // Boolean 타입인 이유는 가져온 Bucket이 null일 경우 nullPointException이 일어날 수 있기 때문에
    // 안전하게 처리한다. 암튼 가져와서 비교하고 이미 예약 되어있으면 false를 반환한다.
    private boolean hasUserAlreadyBooked(String userId, String showId) {
        RBucket<Boolean> b = redisson.getBucket("user_booking:" + userId + ":" + showId);
        return Boolean.TRUE.equals(b.get());
    }   // func end


    // 대기열 등록 (enqueue) 메소드
    /**
     * 사용자가 "예매 페이지 입장 시도"를 하면 호출됨
     * 1 이미 예매한 사용자라면 대기열에 넣지 않음
     * 2 아직 예매하지 않았다면 대기열에 추가
     * 3. 빈 자리가 있으면 assignNextIfPossible()로 토큰 발급 시도
     */
    public EnqueueResult enqueue(String userId, String showId) {
        // 이미 예매한 사용자인 경우 즉시 차단함 ===> 매크로등 쓸데 없는 대기열 점유를 방지한다.
        if (hasUserAlreadyBooked(userId, showId)) {
            return new EnqueueResult(false, 0);
        }   // if end

        // 대기열(큐)에 유저를 추가한다.
        queue().add(userId);

        // 빈 슬롯이 있다면 즉시 다음 사람을 입장시킴
        // 아래에 메소드 호출임 오해 금지.
        assignNextIfPossible();

        // 현재 대기열 길이 반환
        // 맨 아래의 생성자임
        return new EnqueueResult(true, queue().size());
    }   // func end


    // 다음 대기자에게 입장 토큰 발급하는 메소드
    /**
     * - 세마포어 퍼밋이 남아있을 때 호출됨
     * - 대기열의 맨 앞 사용자를 꺼내서
     *   입장 토큰(admission token)을 발급한다.
     * - 발급된 토큰은 30초 동안만 유효하며,
     *   이 시간 안에 프론트에서 "/enter" 요청을 보내야 입장 확정됨.
     */
    public void assignNextIfPossible() {
        try {
            // 세마포어 자리가 남아 있지 않으면 (입장 인원 꽉 참) 리턴한다.
            if (semaphore().availablePermits() <= 0) return;

            // 대기열에서 맨 앞 유저를 꺼냄
            String nextUser = queue().poll();
            // 대기하는 인원이 존재하지 않으면 그냥 종료
            if (nextUser == null) return;

            // 세마포어 퍼밋 1개 확보를 시도한다.
            // (tryAcquire() : 현재 세마포어에 여유 슬롯이 있다면 하나를 즉시 획득하고, 없으면 false 를 반환
            boolean acquired = semaphore().tryAcquire();
            // 만약 실패한다면?
            if (!acquired) {
                // 다시 큐에 되돌림 >> 잘가라~ 그리고 리턴
                queue().add(nextUser);
                return;
            }   // if end

            // 1회용 토큰 생성 (UUID) 나중에 jjwt로 해도 됨.
            String token = UUID.randomUUID().toString();

            // Redis에 토큰 userId를 저장한다 ==> 30초 TTL를 준다
            redisson.getBucket(RedisKeys.ADMISSION_PREFIX + token)
                    .set(nextUser, ADMISSION_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

            // ===== [SSE 추가] 브라우저로 실시간 입장 신호 전송 =====
            sendAdmissionSignal(nextUser, token);

        } catch (Exception e) {
            e.printStackTrace();
        }   // try end
    }   // func end

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

    // ===============================================================
    // ===== [SSE 추가] 유틸 메소드
    // ===============================================================

    /**
     * 브라우저가 구독을 시작할 때 호출할 SSE 연결 함수
     * - 컨트롤러에서 GET /gate/subscribe/{userId} 로 노출하여 사용
     * - 연결 타임아웃은 세션TTL + 토큰TTL 여유로 설정
     */
    public SseEmitter connectSse(String userId) {
        long timeoutMs = TimeUnit.MINUTES.toMillis(SESSION_MINUTES) + TimeUnit.SECONDS.toMillis(ADMISSION_TOKEN_TTL_SECONDS);
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((ex) -> emitters.remove(userId));

        return emitter;
    }

    /**
     * 토큰 발급 시 해당 유저 브라우저로 실시간 푸시
     * - 이벤트명: "admission"
     * - data: 토큰 문자열 (JSON 직렬화 규칙에 맞춰 전송)
     */
    private void sendAdmissionSignal(String userId, String token) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("admission")
                    .data(token));
            emitter.complete(); // 일회성 이벤트 후 연결 종료 (원하면 주석처리하여 유지 가능)
            emitters.remove(userId);
        } catch (IOException e) {
            emitters.remove(userId);
        }
    }
}
