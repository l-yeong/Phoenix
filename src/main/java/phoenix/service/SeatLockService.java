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


@Service
@EnableScheduling
@RequiredArgsConstructor
//  좌석 클릭 시마다 좌석에 모든 것을 처리하는 서비스
public class SeatLockService {  // class start

    // 의존성 추가
    private final SeatsMapper seatsMapper;

    /// redisson configuration 의존성 주입
    private final RedissonClient redisson;

    /// 선택한 좌석 유지 시간 : 클릭 후 2분 유지 ( 임시 시간 2분으로 측정 )
    private static final long HOLD_TTL_SECONDS = 120;

    /// 사람 당 최대로 선택할 수 있는 최대 좌석 수 ( 임시로 4개가 최대 )
    private static final int MAX_SEATS_PER_USER = 4;

    // Redis 객체들 가져오는 부분

    // return 값은 ( Redis.해당메소드 ) 를 만들고 그 안에 임의의 저장할 이름을 넣어준다. 예를 들어 SEAT_HOLD_MAP => 내가 지정한 임의 이름
    // RMapCache<K, V> => TTL 지원 Map 형태임, put에 key랑 value를 넣고 만료시간과 만료 타입을 넣어주면 됨.
    private RMapCache<String, String> holdMap() { return redisson.getMapCache(RedisKeys.SEAT_HOLD_MAP); }
    // RSet<V> => Redis 중복 없는 집합, 중복을 방지한다!
    private RSet<String> soldSet(String gno) { return redisson.getSet(RedisKeys.SEAT_SOLD_SET + ":" + gno); }
    // RSetCache<V> : TTL 정할 수 있는 집합
    private RSetCache<String> userHoldSet(int mno, int gno) { return redisson.getSetCache("user:hold:" + mno + ":" + gno); }
    // RLock => 분산 락 , 여러 요청에서 단 하나만 선택하여 락을 거는 것.
    private RLock seatLock(String seatKey) { return redisson.getLock("seat:lock:" + seatKey); }
    // 좌석 유틸 키
    private String seatKey(String gno, String sno) { return gno + ":" + sno; }

    // 메소드 부분

    // 현재 유저가 게이트세션 을 보유 중인지 확인하고, 없을 시에는 좌석에 대한 권한을 부여하지 않는 헬퍼 메소드
    private boolean hasActiveSession(int mno) {
        // session:{mno} -> "alive" 를 TTL 로 가지고 있으면 true
        // getBucket => 레디슨에 해당 단일 객체 참조주소값 가져옴.
        // get(), set() , isExists()등 매소드를 할당해줌
        return redisson.getBucket(RedisKeys.SESSION_PREFIX + mno).isExists();
    }   // func end

    // 중복 예매 여부 확인 (공연/회차 단위)
    // getBucket으로 참조주소값을 가져오고 그 값을 Bucket<V> 타입의 b에 저장한다.
    // 그것을 .get()해서 Redis 타입을 자동 파싱해서 자바 형식으로 가져와 true인지 false인지 확인한다.
    // Boolean 타입인 이유는 가져온 Bucket이 null일 경우 nullPointException이 일어날 수 있기 때문에
    // 안전하게 처리한다. 암튼 가져와서 비교하고 이미 예약 되어있으면 false를 반환한다.
    private boolean hasUserAlreadyBooked(int mno, int gno) {
        RBucket<Boolean> b = redisson.getBucket("user_booking:" + mno + ":" + gno);
        return Boolean.TRUE.equals(b.get());
    }   // func end


    // 예매 완료 시 기록
    private void markUserAsBooked(int mno, int gno) {
        // 6시간 캐시 (원하면 공연 시작 시각까지 남은 시간으로 설정)
        // getBucket => 레디슨에 해당 단일 객체 참조주소값 가져옴.
        // () 그 안에값은 그 객체의 key
        // .set => 안에 value를 지정. true고 6시간동안 ttl 설정
        redisson.getBucket("user_booking:" + mno + ":" + gno)
                .set(true, 6, TimeUnit.HOURS);
    }   // func end


    @PostConstruct
    public void initSeatListCache() {
        try {
            List<String> template = seatsMapper.findAllSeatNames();
            if (template == null || template.isEmpty()) {
                System.out.println("[SeatLockService] 초기화: 좌석 데이터 없음");
                return;
            }

            RSet<String> templateSet = redisson.getSet("allSeats:TEMPLATE");
            if (templateSet.isEmpty()) {
                templateSet.addAll(template);
            }
            System.out.println("[SeatLockService] 좌석 템플릿 캐시 완료 (size=" + templateSet.size() + ")");

        } catch (Exception e) {
            System.out.println("[SeatLockService] 좌석 목록 초기화 실패: " + e.getMessage());
        }
    }



    // 좌석 선택 메소드 => 프론트에서 좌석 버튼 클릭 시마다 메소드 실행함
    // 실패 경우는  세션 없음 / 매진석 / 이미 임시 4좌석 보유 / 락 경쟁으로 인하여 내가 실패한 경우
    public int tryLockSeat(int mno, int gno, String sno) throws InterruptedException {

        // 게이트에 없으면 false(=실패) 반환 (보안)
        if (!hasActiveSession(mno)) return -1;

        // 만약 중복 예매라면 실패 반환
        if (hasUserAlreadyBooked(mno, gno)) return -2;

        // 해당 선택한 좌석이 매진된 좌석이면 false 반환(실패)
        if (soldSet(String.valueOf(gno)).contains(sno)) return -3;

        // 해당 유저가 이미 4좌석을 선택했다면 선택 불가 false 반환(실패)
        // userHoldSet 즉, mno가 들어가 있는 집합을 만들어서 그것을 myHolds의 변수에 대입
        RSetCache<String> myHolds = userHoldSet(mno , gno);

        // 그 유저의 집합(좌석 수를 관리함) 것의 size 즉 좌석 갯수가 내가 지정한 4개의 좌석보다 크면 실패, size는 0부터 세는 것을 참고하자
        if (myHolds.size() >= MAX_SEATS_PER_USER) return -4;

        // 좌석 키 생성 (gno:sno)
        String key = seatKey(String.valueOf(gno), sno);

        // 좌석에 대한 분산락 (RLock)을 즉시 시도한다.
        RLock lock = seatLock(key);

        // tryLock( a, b, c ); => a는 다른 이가 락을 잡고 있다면 다른 건 얼마나 기다릴까? 라는 것임, 0이니까 락 걸려있으면 바로 실패 내기
        // b는 이 락을 얼마나 걸어둘 것인가?  => 내가 설정한 120초 즉 2분을 기다림, 즉 이 좌석을 내가 선택했다면 2분만 임시상태로 내 것
        // c는 b에 대한 시간의 단위를 나타냄, 여기서는 SECONDS 이니까 초를 가르킴!!!!
        boolean acquired = lock.tryLock(0, HOLD_TTL_SECONDS, TimeUnit.SECONDS);
        // 만약 누가 먼저 눌러서 내가 락을 못 잡은 상태면 실패 반환, 동시성을 보장한다!
        if (!acquired) {
            return -3;
        }   // if end

        // 락 성공 했으므로 내 임시 좌석으로 기록한다. 만료는 TTL=120초, 시간이 지나면 자동 만료됨!
        // holdMap()은 위에 내가 정의한 것 , 기록할 것은 좌석, 내id, 내가 정의한 120, 초로 단위를 맞춤
        holdMap().put(key, String.valueOf(mno), HOLD_TTL_SECONDS, TimeUnit.SECONDS);

        // 내 좌석 정보 Redisson 집합에 좌석 정보를 기록한다. => 4좌석 초과면 막아야 하니까
        myHolds.add(sno, HOLD_TTL_SECONDS, TimeUnit.SECONDS);

        // 성공 반환
        return 1;

    }   // func end

    // 좌석 해제 메소드 => 내가 잡아둔 좌석을 다시 클릭하면 해제하는 메소드
    public boolean releaseSeat(int mno, String sno, int gno) {

        // 좌석 키 생성 (gno:sno)
        String key = seatKey(String.valueOf(gno), sno);

        // 내가 임시로 소유 중인지 확인한다. (남의 좌석 해제 금지) , holdMap() 호출한다 해당 좌석을
        String holder = holdMap().get(key);
        // holder가 없거나 holder의 유저 정보가 일치하지 않으면 해제 실패
        if (holder == null || !holder.equals(String.valueOf(mno))) return false;

        // 임시 소유 좌석 정보를 제거한다.
        holdMap().remove(key);

        // 해당 유저의 좌석 정보를 삭제한다. 좌석 취소하고 4개 좌석 중 다른 좌석을 선택해야 하는 경우가 있기 때문
        userHoldSet(mno , gno).remove(sno);

        // 좌석에 대한 락 해제한다. 다른 사람이 예매 해야함.
        try {
            // 그 좌석에 걸려 있는 락을 가져온다.
            RLock lock = seatLock(key);
            // holder 검사 통과했으니 안전하게 강제 해제
            lock.forceUnlock();
        } catch (Exception ignore) {}   // 예외 처리 안함

        // 다 했으면 성공 반환
        return true;

    }   // func end


    // 결제 메소드
    // 성공 한 경우 SOLD set 에 등록하고 holdMap/userHoldSet 에서 제거 db 저장함.
    public boolean confirmSeats(int mno, List<String> snos, int gno, StringBuilder failReason ) {

        // 게이트 체크함, 이유는 게이트 만료 후 구매를 방지한다.
        if (!hasActiveSession(mno)) { failReason.append("not exist session"); return false; }

        // 중복 예매 최종 방어
        if (hasUserAlreadyBooked(mno, gno)) {
            failReason.append("already booked this show");
            return false;
        }

        // 유효성 검사 => 내가 찜한 좌석인지 , 혹은 자바 오류로 매진이 아닌지
        for (String sno : snos) {

            // 만약 Redis 매진 집합에 좌석넘버가 포함되어있다면 실패 반환
            if (soldSet(String.valueOf(gno)).contains(sno)) { failReason.append(sno).append("already sold"); return false; }

            // 좌석 키 설정함
            String key = seatKey(String.valueOf(gno), sno);
            // 임시 찜한 좌석을 가져온다.
            String holder = holdMap().get(key);

            // holder가 없거나 그것이 유저정보랑 일치하지 않으면
            if (holder == null || !holder.equals(String.valueOf(mno))) {
                // 실패 반환
                failReason.append(sno).append(" not held by you; ");
                return false;
            }   // if end
        }   // for end

        /// 모든게 괜찮다면 구매처리, 임시 소유 제거 => 꼭 !!!! db 에도 해야함!!! // 추가해야함.
        for (String sno : snos) {
            // 매진 집합에 추가한다
            soldSet(String.valueOf(gno)).add(sno);
            // 키 설정하기
            String key = seatKey(String.valueOf(gno), sno);
            // 임시 소유 제거한다
            holdMap().remove(key);
            // 내 임시 목록에서 제거한다.
            userHoldSet(mno , gno).remove(sno);
            // 즉시 매진 처리
            try { seatLock(key).forceUnlock(); } catch (Exception ignore) {}
        }   // for end

        // 공연 단위 “이미 예매” 기록
        markUserAsBooked(mno, gno);

        return true;
    }   // func end

    // 게이트 세션 만료된 유저의 좌석 자동 해제 메소드
    @Scheduled(fixedDelay = 2000)
    public void cleanupExpiredSeatHolds() {
        try {

            // 모든 좌석 점유 정보 조회
            // map을 순회해서 하나하나 꺼내는 향상된 for문
            for (Map.Entry<String, String> entry : holdMap().entrySet()) {
                // 좌석이랑 유저 정보 다 꺼냄 반복문 순회마다
                String key = entry.getKey();     // seatKey
                String mno = entry.getValue();

                // 해당 사용자의 게이트 세션 존재 여부 확인
                // getBucket() => Redis 에서 해당 key에 맞는 value 값을 객체로 가지고
                // get(), set() , isExists()등 매소드를 할당해줌
                // isExists() : key가 존재하나 묻는 메소드
                boolean alive = redisson.getBucket(RedisKeys.SESSION_PREFIX + mno).isExists();

                // 세션 만료된 사용자만 정리
                if (!alive) {
                    // indexof(:) => :의 문자 위치를 찾음
                    int idx = key.indexOf(':');
                    // :의 문자가 맨끝이거나 맨 처음이면 오류니까 그냥 제거함.
                    if (idx <= 0 || idx >= key.length() - 1) {
                        // 포맷 이상 시 안전하게 그냥 제거
                        holdMap().remove(key);
                        continue;
                    }   // if end

                    // 각각 정보들을 가져온다.
                    String gno = key.substring(0, idx);
                    String sno = key.substring(idx + 1);

                    holdMap().remove(key);        // 좌석 hold 제거
                    userHoldSet(Integer.parseInt(mno) , Integer.parseInt(gno)).remove(sno); // 내가 게이트 해제 전 좌석 보유한 것들도 보유 목록에서도 제거

                    try {
                        // 소유자와 무관하게 좌석 락 즉시 해제
                        seatLock(key).forceUnlock();
                    } catch (Exception ignore) {}

                }   // if end
            }   // for end

        } catch (Exception e) {
            System.out.println(e);
        }   // try end

    }   // func end

    public Map<String, String> getSeatStatusMap(int gno, int mno) {
        String showKey = String.valueOf(gno);

        // ✅ 경기별 세트 대신 템플릿을 사용
        RSet<String> allSeatsTemplate = redisson.getSet("allSeats:TEMPLATE");

        RSet<String> soldSet = soldSet(showKey);                 // gno별 매진 좌석
        RMapCache<String, String> holdMap = holdMap();           // gno:sno -> mno

        Map<String, String> result = new LinkedHashMap<>();

        for (String seatName : allSeatsTemplate) {
            String seatKey = seatKey(showKey, seatName);

            if (soldSet.contains(seatName)) {
                result.put(seatName, "SOLD");
                continue;
            }
            String holder = holdMap.get(seatKey);
            if (holder != null) {
                result.put(seatName, holder.equals(String.valueOf(mno)) ? "HELD_BY_ME" : "HELD");
                continue;
            }
            result.put(seatName, "AVAILABLE");
        }
        return result;
    }




}   // class end
