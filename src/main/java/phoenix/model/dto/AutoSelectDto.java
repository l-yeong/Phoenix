package phoenix.model.dto;

import lombok.*;

import java.util.List;

public class AutoSelectDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AutoSelectReq {
        private int gno;                    // 경기번호
        private int qty;                    // 요청 매수(1~4)
        private boolean preferContiguous;   // 연석 우선
        private String fanSide;             // "HOME" | "AWAY" | "ANY"
        private Boolean crossZone;          // 멀티존 폴백 허용(없으면 true로 처리)
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AutoSelectRes {
        private boolean ok;
        private String reason;      // 실패/부분 사유
        private String strategy;    // 탐색 로그(디버깅용)

        private Integer ttlSec;     // 클라이언트 표시용 TTL(초) — 보유 TTL과 맞춤(120)
        private Integer qty;        // 요청 매수
        private Integer qtyHeld;    // 실제 확보 수(부분확보 시 < qty)

        // bundles가 1개면 단일존 성공 → 프론트에서 즉시 이동
        private List<Bundle> bundles;

        // 레거시 단일존 응답 호환(프론트가 아직 참조한다면)
        private Integer zno;
        private List<Integer> heldSnos;
        private Boolean contiguous;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Bundle {
        private int zno;
        private String zoneLabel;
        private boolean contiguous;      // 이 번들의 좌석이 연석인지
        private List<Integer> snos;
        private List<String> seatNames;
    }
}
