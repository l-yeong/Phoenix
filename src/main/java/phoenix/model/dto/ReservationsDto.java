package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReservationsDto {

    private int rno;                // 예매번호
    private int mno;                // 예매 회원
    private int sno;                // 예매 좌석
    private String reserved_at;     // 예매 시각
    private String status;          // 상태

}//func end
