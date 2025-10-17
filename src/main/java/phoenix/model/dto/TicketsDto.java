package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TicketsDto {

    private int tno;                // 티켓번호
    private int rno;                // 관련예매번호
    private String ticket_code;     // 티켓 고유 코드
    private String issued_at;       // 발급일
    private boolean valid;           // 유효여부
    private int price;              // 가격

    private int gno;
    private LocalDateTime dateTime;

}//func end
