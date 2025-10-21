// src/main/java/phoenix/dto/SeatsDto.java
package phoenix.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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


    @Data public static class SeatRef { int zno; int sno; }

    @Data public static class StatusReq { int gno; List<SeatRef> seats; }

    @Data public static class SingleSeatReq { int gno; int zno; int sno; }

    // ✅ 반드시 static! (non-static 이면 Jackson이 생성 못 해서 500)
    @Data
    @NoArgsConstructor   // ✅ Jackson용 기본 생성자
    @AllArgsConstructor
    public static class ConfirmReq {
        // wrapper 타입 권장: null 체크/검증에 유리
        private Integer gno;
        private List<Integer> snos;
    }
}
