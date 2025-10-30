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
    private int gno;                // 경기 번호
    private String reserved_at;     // 예매 시각
    private String status;          // 상태

    private int count;           // 몇 개의 좌석이 예매가 되어있는지 좌석 락에서 확인용 멤버변수
    private int zno;            // 존 넘버 => 예매 취소에 필요

    // NEW: 예매 채널 (general | senior)
    private String channel;
}//func end
