package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class GateDto {

    /** 대기열 등록 요청 */
    @Data
    public static class EnqueueRequest {
        private int gno; // 공연/경기 식별자
    }

    /** 대기열 등록 응답 */
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class EnqueueResponse {
        private boolean queued;   // true면 큐 등록, false면(이미 예매 등) 차단
        private int waiting;      // 현재 대기열 길이
    }

    /** 퇴장 요청(선택: 바디/쿼리 어느 쪽이든 gno 필요) */
    @Data
    public static class LeaveRequest {
        private int gno;
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
