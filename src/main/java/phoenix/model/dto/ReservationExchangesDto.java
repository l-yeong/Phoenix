package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReservationExchangesDto {

    private int exno;               // 교환번호
    private int from_rno;           // 교환 요청 예매번호
    private int to_rno;             // 교환 대상 예매번호
    private String status;          // 교환 상태
    private String requested_at;    // 요청 시각
    private String responded_at;    // 응답 시각
    private int from_mno;           // 요청자 회원번호
}//func end
