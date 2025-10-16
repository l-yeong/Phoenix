package phoenix.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * [SeatDto]
 * - SeatLockService <-> Controller <-> Front 간 데이터 모델
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
     * - showId는 "공연/회차 식별자"로 중복 예매 방지에 사용됨
     *
     * JSON 예시:
     * {
     *   "userId": "u123",
     *   "showId": "SHOW-2025-10-16-19:00",
     *   "seatId": "A1"
     * }
     */
    @Data
    public static class SelectRequest {
        private String userId;
        private String showId;
        private String seatId;
    }

    /**
     * [SelectResponse]
     * - 좌석 선택 성공/실패 결과
     *
     * JSON 예시:
     * { "ok": true, "message": "locked" }
     */
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class SelectResponse {
        private boolean ok;        // true: 선택 성공, false: 실패
        private String message;    // "locked" | "already held/sold" | "session missing" 등
    }


    // ------------------------------------------------------------
    // 2) 좌석 해제 요청/응답
    // ------------------------------------------------------------

    /**
     * [ReleaseRequest]
     * - 이미 내가 잡은 좌석을 다시 클릭해서 해제할 때 보냄
     *
     * JSON 예시:
     * { "userId": "u123", "seatId": "A1" }
     */
    @Data
    public static class ReleaseRequest {
        private String userId;
        private String seatId;
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
     *   "userId": "u123",
     *   "showId": "SHOW-2025-10-16-19:00",
     *   "seatIds": ["A1","A2","B1"]
     * }
     */
    @Data
    public static class ConfirmRequest {
        private String userId;
        private String showId;
        private List<String> seatIds;
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
    // 4) 좌석 상태 맵 요청/응답 (선택 기능: 주석 참고)
    // ------------------------------------------------------------

    /**
     * [MapRequest] (선택)
     * - 특정 좌석 목록의 상태를 조회하고 싶을 때 사용
     * - seatIds가 null/빈배열이면 서버 기본 풀을 사용(서비스 구현에 따라)
     */
    @Data
    public static class MapRequest {
        private List<String> seatIds;
    }

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
}
