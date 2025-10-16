package phoenix.util;

// Redis 키를 보관하는 클래스
public class RedisKeys {    // class start

    // ===== Gate(입장/대기열) =====
    public static final String GATE_SEMAPHORE = "gate:semaphore";           // 동시 입장 세마포어(전역 5명)
    public static final String WAITING_QUEUE  = "gate:waiting:queue";       // 대기열(FIFO)
    public static final String ADMISSION_PREFIX = "gate:admission:";        // admission:{token} -> userId (1회용/TTL)
    public static final String SESSION_PREFIX   = "gate:session:";          // session:{userId} -> "alive" (TTL 유지)
    public static final String EXTENDED_PREFIX  = "gate:extended:";         // extended:{userId} -> true/false
    public static final String ACTIVE_SET       = "gate:active:users";      // 현재 입장 중 사용자 Set

    // ===== Seat(좌석) =====
    public static final String SEAT_HOLD_MAP   = "seat:hold:map";           // RMapCache: seatId -> userId (TTL=임시 점유 시간)
    public static final String SEAT_SOLD_SET   = "seat:sold:set";           // RSet: 확정된 좌석(영구)

}   // class end