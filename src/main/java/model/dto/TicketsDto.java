package model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TicketsDto {

    private int tno;                // 티켓번호
    private int rno;                // 관련예매번호
    private String ticket_code;     // 티켓 고유 코드
    private String issued_at;       // 발급일
    private String valid;           // 유효여부
    private int price;              // 가격

}//func end
