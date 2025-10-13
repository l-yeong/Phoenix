package phoenix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import phoenix.model.mapper.TicketsMapper;
import org.springframework.stereotype.Service;
import phoenix.util.TicketsQR;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TicketsService {
    private final TicketsMapper ticketsMapper;

    public List<byte[]> ticketPrint(int mno) {
        List<Map<String, Object>> result = ticketsMapper.ticketPrint(mno);

        // Map 한글 변환
        List<Map<String, Object>> nameChange = result.stream()
                .filter(map->"reserved".equals(map.get("reservation_status")))
                .map(map -> {Map<String, Object> newMap = new LinkedHashMap<>(); //순서보장 HashMap
                    newMap.put("티켓코드", map.get("ticket_code"));
                    newMap.put("이름", map.get("mname"));
                    newMap.put("연락처", map.get("mphone"));
                    newMap.put("이메일", map.get("email"));
                    //사용여부
                    Boolean valid = (Boolean)map.get("valid");
                    newMap.put("사용여부", valid!=null&& valid? "미사용" : "사용불가");
                    newMap.put("예약상태", map.get("reservation_status"));
                    newMap.put("가격", map.get("ticket_price"));
                    newMap.put("구역", map.get("zname"));
                    newMap.put("좌석", map.get("seat_no"));
                    return newMap;
                }).toList(); //List end

        // 정리된 JSON 출력
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // 정리 출력 옵션
        nameChange.forEach(map -> {
            try {
                System.out.println(mapper.writeValueAsString(map));
            } catch (Exception e) {
                e.printStackTrace();
            }//catch end
        }); //forEach end

        return nameChange.stream()
                .map(TicketsQR::TicketQrCode)
                .toList();
    }//func end

}//func end
