package phoenix.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import phoenix.util.RedisKeys;

import java.util.concurrent.TimeUnit;


/**
 * ===============================================================
 * [GateService]
 * 🎟️ 공연 예매 입장 제어(대기열 + 동시성 제어) 핵심 로직
 * ===============================================================

 * 핵심 역할
 * 1. 동시에 입장 가능한 사용자 수를 제한 (예: 3명만)
 * 2. 나머지 인원은 "대기열"에 넣어 순서대로 입장시킴
 * 3. 입장 시 "1회용 토큰"을 발급하여 프론트에 전달
 * 4. 입장 중(게이트 통과)인 사용자는 TTL(세션 유지 시간) 동안만 예매 가능
 * 5. 이미 예매한 사용자는 대기열에 들어가지 못하도록 차단

 * 기술 요소
 * - Redisson RSemaphore : 동시 입장 인원(퍼밋) 제어
 * - Redisson RBlockingQueue : 대기열(FIFO)
 * - Redisson RBucket : 세션 상태(userId), 토큰 저장(token → userId)
 * - Redisson RSet : 현재 활성 사용자 목록 관리

 * TTL 설정
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



    // Redis 구조 접근자

    // 동시 입장 제한 세마포어
    // "gate:semaphore" 키에 연결된 Redisson 분산 세마포어
    // 퍼밋이 0이면 모든 입장이 꽉 찬 상태 ,,,  세마포어란 쉽게 말하면 쓰레드풀임.
    private RSemaphore semaphore() { return redisson.getSemaphore(RedisKeys.GATE_SEMAPHORE); }

    // 대기열 큐 => 대기자를 순차적으로 들여보냄 , 분산 대기열임.
    private RBlockingQueue<String> queue() { return redisson.getBlockingQueue(RedisKeys.WAITING_QUEUE); }

    // 현재 게이트 안에 있는 사용자 Set 집합
    private RSet<String> activeSet() {return redisson.getSet(RedisKeys.ACTIVE_SET);}
    // 대기 명단
    private RSet<String> waitingSet() { return redisson.getSet(RedisKeys.WAITING_SET); }


    // 초기화 로직 ==> 앱 스타트 시 3개의 퍼밋만 세팅
    // 스프링(Spring)에서 “빈(Bean)이 생성되고 DI(의존성 주입)가 끝난 직후 한 번만 자동 실행되는 초기화 어노테이션
    @PostConstruct
    public void init() {
        // Redis 상에 퍼밋 개수를 지정 (이미 존재해도 무해)
        semaphore().trySetPermits(MAX_PERMITS);
    }   // func end


    // 메소드 부분

    // 중복 예매 여부 확인 메소드
    // getBucket 으로 참조주소값을 가져오고 그 값을 Bucket<V> 타입의 b에 저장한다.
    // 그것을 .get()해서 Redis 타입을 자동 파싱해서 자바 형식으로 가져와 true 인지 false 인지 확인한다.
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

        // 이미 대기중이면 재등록하지 않음
        if (!waitingSet().add(userId)) return new EnqueueResult(true, queue().size());

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
     세마포어 퍼밋이 남아있을 때 호출됨
     대기열의 맨 앞 사용자를 꺼내서
     입장 토큰(admission token)을 발급한다.
     발급된 토큰은 30초 동안만 유효하며,
     이 시간 안에 프론트에서 "/enter" 요청을 보내야 입장 확정됨.
     */
    public void assignNextIfPossible() {
        try {
            // 세마포어 자리가 남아 있지 않으면 (입장 인원 꽉 참) 리턴한다.
            if (semaphore().availablePermits() <= 0) return;

            // 대기열에서 맨 앞 유저를 꺼냄
            String nextUser = queue().poll();
            // 대기하는 인원이 존재하지 않으면 그냥 종료
            if (nextUser == null) return;
            // 이미 활성 유저라면 재입장 방지 로 인하여 그냥 종료
            if (activeSet().contains(nextUser)) return;

            // 세마포어 퍼밋 1개 확보를 시도한다.
            // (tryAcquire() : 현재 세마포어에 여유 슬롯이 있다면 하나를 즉시 획득하고, 없으면 false 를 반환
            boolean acquired = semaphore().tryAcquire();
            // 만약 실패한다면?
            if (!acquired) {
                // 다시 큐에 되돌림 >> 잘가라~ 그리고 리턴
                queue().add(nextUser);
                return;
            }   // if end

            // Redis에 토큰 userId를 저장한다 ==> 30초 TTL를 준다
            redisson.getBucket(RedisKeys.SESSION_PREFIX + nextUser)
                    .set("alive", SESSION_MINUTES, TimeUnit.MINUTES);

            // 활성 유저명단에 넣음
            activeSet().add(nextUser);
            // assignNextIfPossible 안에서 입장 처리 직후 제거
            waitingSet().remove(nextUser);

        } catch (Exception e) {
            e.printStackTrace();
        }   // try end
    }   // func end

    // 프론트에서 확인할 입장했는지 확인용 메소드
    public boolean isEntered(String userId) {
        return redisson.getBucket(RedisKeys.SESSION_PREFIX + userId).isExists();
    }   // func end


    // [퇴장 처리용 메소드 => 사용자가 예매를 끝내거나 퇴장할 때 실행.
    /**
     사용자가 예매를 마치거나 직접 퇴장할 때 호출됨.
     session:{userId} 키 삭제
     activeSet 에서 제거
     세마포어 퍼밋 1개 반환 후 다음 대기자에게 기회 부여
     */
    public boolean leave(String userId) {
        // 세션 및 활성 사용자 제거
        // delete() => 해당 버켓 제거
        redisson.getBucket(RedisKeys.SESSION_PREFIX + userId).delete();

        // 지금 입장 중인 유저 목록(activeSet) 에서 빼고, 원래 들어있었는지 결과를 wasActive로 받음 (있었으면 true)
        boolean wasActive = activeSet().remove(userId);

        // 세마포어에 있는 퍼밋을 제거함
        if (wasActive) {
            try {
                semaphore().release();
            } catch (Exception ignore) {
            }
        }
        // 큐에 있는 1순위 사람을 퍼밋에 불러오는 메소드 실행
        assignNextIfPossible();

        // 성공 반환
        return true;
    }   // func end


    // 상태 조회용 헬퍼
    // 대기 인원수 조회 메소드
    public int waitingCount() {
        // 현재 큐에 대기 중인 사람 수
        return queue().size();
    }   // func end

    // 남은 입장 가능 슬롯 수 알려주는 메소드
    public int availablePermits() {
        // 현재 남은 입장 가능 슬롯 수
        return semaphore().availablePermits();
    }   // func end


    // 1분 연장 하는 메소드
    /**
     사용자가 "연장하기" 버튼을 눌렀을 때 호출됨.
     session:{userId} 의 TTL(남은 수명)을 1분 더 늘려준다.
     연장은 최대 N회로 제한할 수도 있음 (원하면 구현 가능)
     */
    public int extendSession(String userId) {
        try {
            // 세션 버킷을 가져옴
            RBucket<String> sessionBucket = redisson.getBucket(RedisKeys.SESSION_PREFIX + userId);

            // 세션이 존재하지 않으면 (만료된 경우) 연장이 불가능
            if (!sessionBucket.isExists()) return 0;

            // 연장 횟수 카운트 버킷 가져오기
            RBucket<Integer> countBucket = redisson.getBucket("gate:extendCount:" + userId);
            // null 값이 뜰 수도 있고 그냥 Integer로 가져옴
            Integer count = countBucket.get();
            // 만약 null 이면 0으로 함
            if (count == null) count = 0;

            // 이미 2회 연장했다면 안됨
            if (count >= 2) return 2;

            // 현재 남은 TTL 가져와서 1분 추가
            // remainTimeToLive() : TTL 시간 가져옴
            long remain = sessionBucket.remainTimeToLive();
            long extended = remain + TimeUnit.MINUTES.toMillis(1);

            // TTL 재설정 (기존 TTL + 1분)
            // expire() 은 이미 있는 버켓에 TTL 다시 설정한다는거임. expire.(int시간 , 시간 단위)
            sessionBucket.expire(extended, TimeUnit.MILLISECONDS);

            // 연장 횟수 +1 저장 (TTL은 세션과 동일하게 설정)
            countBucket.set(count + 1, SESSION_MINUTES, TimeUnit.MINUTES);

            System.out.println("[GateService] " + userId + " 님 세션 연장 (" + (count + 1) + "/2)");

            return count; // 연장 성공
        } catch (Exception e) {
            return -1;
        }   // try end
    }   // func end



    // 스케줄러 : 세션 만료 회수 메소드
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
                // session:{userId} 키가 여전히 존재하는지 검사하기 위해 버켓을 가져옴.
                boolean alive = redisson.getBucket(RedisKeys.SESSION_PREFIX + uid).isExists();

                // 존재하지 않으면 TTL 만료된 것 → 회수 처리
                if (!alive) {
                    // 없앰
                    activeSet().remove(uid);
                    // 세마포어 퍼밋 삭제
                    try { semaphore().release(); } catch (Exception ignore) {}
                    // 다음 대기자 입장 시도
                    assignNextIfPossible();
                }   // if end
            }   // for end
        } catch (Exception e) {
            e.printStackTrace();
        }   // try end
    }   // func end

    // ===============================================================
    // 내부 결과 DTO(record) 이거 반환하고 컨트롤러에서 실제 dto에 넣어 쓸 예정
    // record는 그냥 dto 같이 생성해줌
    public record EnqueueResult(boolean queued, int waiting) {}
}   // class end
