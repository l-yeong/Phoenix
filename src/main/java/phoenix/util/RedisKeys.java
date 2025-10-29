package phoenix.util;

// Redis 키를 보관하는 클래스 (gno 스코프 버전)
public class RedisKeys {

    // ===== Gate =====
    public static final String GATE_GNO_INDEX        = "gate:gno:index";
    public static final String GATE_SEMAPHORE_PREFIX = "gate:%d:semaphore";
    public static final String WAITING_QUEUE_PREFIX  = "gate:%d:waiting:queue";
    public static final String WAITING_SET_PREFIX    = "gate:%d:waiting:set";
    public static final String ACTIVE_SET_PREFIX     = "gate:%d:active:set";
    public static final String SESSION_PREFIX        = "gate:%d:session:%d";

    // ===== Seats =====
    public static final String SEAT_HOLD_MAP = "seat:hold:map";
    public static final String SEAT_SOLD_SET = "seat:sold:set";

    // ===== Senior counters (NEW) =====
    public static final String SENIOR_BOOKED_PREFIX = "senior:booked:%d:%d";

    // helpers
    public static String keySemaphore(int gno){ return String.format(GATE_SEMAPHORE_PREFIX, gno); }
    public static String keyQueue(int gno){ return String.format(WAITING_QUEUE_PREFIX, gno); }
    public static String keyWaitingSet(int gno){ return String.format(WAITING_SET_PREFIX, gno); }
    public static String keyActiveSet(int gno){ return String.format(ACTIVE_SET_PREFIX, gno); }
    public static String keySession(int gno, int mno){ return String.format(SESSION_PREFIX, gno, mno); }

    // NEW
    public static String keySeniorBooked(int mno, int gno) { return String.format(SENIOR_BOOKED_PREFIX, mno, gno); }
}
