package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [Gate 전용 DTO]
 * - 컨트롤러 <-> 프론트 통신 모델
 * - gno 포함: 공연(또는 회차) 단위 중복 예매 방지에 사용
 */
public class GateDto {

    /** 대기열 등록 요청 */
    @Data
    public static class EnqueueRequest {
        private int mno;
        private int gno; // 같은 공연 중복 예매 사전 차단용
    }

    /** 대기열 등록 응답 */
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class EnqueueResponse {
        private boolean queued;   // true면 큐 등록, false면(이미 예매 등) 차단
        private int waiting;      // 현재 대기열 길이
    }
    /** 입장 확정 요청 */
    @Data
    public static class EnterRequest {
        private int mno;
        private String token;
        private int gno; // 입장 시에도 중복 예매 재확인
    }

    /** 입장 확정 응답 */
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class EnterResponse {
        private boolean allowed;  // true면 세션 시작(입장 성공)
        private String message;   // "entered" / "denied" 등
    }

    /** 퇴장 요청 */
    @Data
    public static class LeaveRequest {
        private int mno;
    }

    /** 퇴장 응답 */
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class LeaveResponse {
        private boolean ok;
    }

    /** 상태 조회 응답 */
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class StatusResponse {
        private int waiting;           // 대기열 길이
        private int availablePermits;  // 남은 퍼밋(빈 슬롯)
    }
}
