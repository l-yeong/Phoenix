// src/main/java/phoenix/dto/SeatsDto.java
package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * [SeatsDto]
 * - SeatLockService <-> SeatController <-> Front 간 데이터 모델
 * - 이 파일만 보면 프론트가 어떤 JSON으로 주고받는지 이해 가능
 * ================================================================
 */
public class SeatsDto {

    // ------------------------------------------------------------
    // 1) 단일 좌석 선택 요청/응답
    // ------------------------------------------------------------

    /**
     * [SelectRequest]
     * - 프론트에서 좌석 버튼(A1 등)을 클릭할 때 보내는 요청
     * - gno는 "공연/회차 식별자"로 중복 예매 방지에 사용됨
     *
     * JSON 예시:
     * {
     *   "mno": "u123",
     *   "gno": "SHOW-2025-10-16-19:00",
     *   "sno": "A1"
     * }
     */
    @Data
    public static class SelectRequest {
        private String mno;
        private String gno;
        private String sno;
    }

    /**
     * [SelectResponse]
     * - 좌석 선택 성공/실패 결과
     * - code: SeatLockService.tryLockSeat(...)의 정수 결과
     *   1   : 성공
     *  -1   : 세션 없음
     *  -2   : 이미 해당 공연 예매함
     *  -3   : 매진/이미 홀드됨/락경쟁 실패
     *  -4   : 보유 한도(4좌석) 초과
     *
     * JSON 예시:
     * { "ok": true, "code": 1, "message": "locked" }
     */
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class SelectResponse {
        private boolean ok;     // true: 성공, false: 실패
        private int code;       // 위 표 참고
        private String message; // 사용자 노출 메시지
    }


    // ------------------------------------------------------------
    // 2) 좌석 해제 요청/응답
    // ------------------------------------------------------------

    /**
     * [ReleaseRequest]
     * - 이미 내가 잡은 좌석을 다시 클릭해서 해제할 때 보냄
     * - 다회차(복수 공연) 고려 시 gno도 함께 받는 버전
     *
     * JSON 예시:
     * { "mno": "1", "gno": "SHOW-2025-10-16-19:00", "sno": "A1" }
     */
    @Data
    public static class ReleaseRequest {
        private String mno;
        private String gno;
        private String sno;
    }

    /**
     * [ReleaseResponse]
     *
     * JSON 예시:
     * { "ok": true }
     */
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ReleaseResponse {
        private boolean ok;
    }


    // ------------------------------------------------------------
    // 3) 좌석 확정(결제 완료) 요청/응답
    // ------------------------------------------------------------

    /**
     * [ConfirmRequest]
     * - 결제하기 버튼 눌렀을 때 프론트가 보내는 요청
     * - 여러 좌석을 한 번에 확정할 수 있음
     *
     * JSON 예시:
     * {
     *   "mno": "u123",
     *   "gno": "SHOW-2025-10-16-19:00",
     *   "seatIds": ["A1","A2","B1"]
     * }
     */
    @Data
    public static class ConfirmRequest {
        private String mno;
        private String gno;
        private List<String> seats;
    }

    /**
     * [ConfirmResponse]
     * - ok=false인 경우 message에 실패 이유가 붙음
     *
     * JSON 예시(성공):
     * { "ok": true, "message": "confirmed" }
     *
     * JSON 예시(실패):
     * { "ok": false, "message": "A1 already sold; B1 not held by you; " }
     */
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ConfirmResponse {
        private boolean ok;
        private String message;
    }


    // ------------------------------------------------------------
    // 4) (선택) 좌석 상태 맵 요청/응답
    // ------------------------------------------------------------

    /**
     * [MapResponse] (선택)
     * - 각 좌석의 상태를 내려줌
     * - status: "AVAILABLE" | "HELD" | "HELD_BY_ME" | "SOLD"
     *
     * JSON 예시:
     * {
     *   "statusBySeat": {
     *     "A1": "HELD_BY_ME",
     *     "A2": "AVAILABLE",
     *     "B1": "SOLD"
     *   }
     * }
     */
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class MapResponse {
        private Map<String, String> statusBySeat;
    }

    // SeatsDto.java 내부에 추가
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class MapRequest {
        private int uno;
        private int gno;

    }
}
