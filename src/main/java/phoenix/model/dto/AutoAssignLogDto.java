package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AutoAssignLogDto {

    private int lno;            // 로그번호
    private int mno;            // 회원번호
    private int gno;            // 해당경기번호(csv/크롤링 매칭)
    private int assigned_zno;    // 배정된 구역
    private String reason;      // 배정이유
    private String create_at;    // 배정시각


}//func end
