package phoenix.util;

// Redis 키를 보관하는 클래스 (gno 스코프 버전)
public class RedisKeys {

    // ===== Gate(입장/대기열) =====
    // 모든 키는 gno별로 분리한다.
    public static final String GATE_GNO_INDEX          = "gate:gno:index";          // 활성 gno 인덱스(스케줄러가 순회)
    public static final String GATE_SEMAPHORE_PREFIX   = "gate:%d:semaphore";       // gno별 동시 입장 세마포어
    public static final String WAITING_QUEUE_PREFIX    = "gate:%d:waiting:queue";   // gno별 대기열(FIFO)
    public static final String WAITING_SET_PREFIX      = "gate:%d:waiting:set";     // gno별 대기자 집합
    public static final String ACTIVE_SET_PREFIX       = "gate:%d:active:set";      // gno별 현재 입장 중 사용자 집합
    public static final String SESSION_PREFIX          = "gate:%d:session:%d";      // session:gno:mno -> "alive" (TTL)

    // ===== Seat(좌석) ===== (그대로 유지)
    public static final String SEAT_HOLD_MAP   = "seat:hold:map";
    public static final String SEAT_SOLD_SET   = "seat:sold:set";

    // 헬퍼: 포맷팅
    public static String keySemaphore(int gno){ return String.format(GATE_SEMAPHORE_PREFIX, gno); }
    public static String keyQueue(int gno){ return String.format(WAITING_QUEUE_PREFIX, gno); }
    public static String keyWaitingSet(int gno){ return String.format(WAITING_SET_PREFIX, gno); }
    public static String keyActiveSet(int gno){ return String.format(ACTIVE_SET_PREFIX, gno); }
    public static String keySession(int gno, int mno){ return String.format(SESSION_PREFIX, gno, mno); }
}
