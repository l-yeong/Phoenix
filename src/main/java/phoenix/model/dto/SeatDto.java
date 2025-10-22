package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SeatDto {

    private int sno;        // 좌석번호
    private int zno;        // 좌석 속한 구역번호
    private String seatName;// 개별 좌석 번호
    private int gno;        // 예매한경기 (csv/크롤링 매칭)
    private String senior;  // 시니어전용 여부

}//func end
