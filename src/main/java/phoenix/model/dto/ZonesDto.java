package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ZonesDto {

    private int zno;        // 구역 번호
    private String zname;   // 구역 이름
    private int price;      // 해당 구역 가격

}//func end
