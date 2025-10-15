package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import phoenix.model.dto.TicketsDto;
import phoenix.model.mapper.TicketsMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TicketsService {
    private final TicketsMapper ticketsMapper;

    //예약 rno가 'reserved' 일 때만 QR 문자열 생성하여 tickets에 테이블에 저장
    @Transactional
    public boolean ticketWrite(int rno) {
        // 예약정보 조회
        Map<String, Object> info = ticketsMapper.ticketPrint(rno);
        if (info == null) return false;

        String status = String.valueOf(info.get("reservation_status"));
        if (!"reserved".equalsIgnoreCase(status)) return false; // 예약상태가 reserved가 아닐 경우 종료


        // 가격조회
        Number seatPrice = (Number) info.get("seat_price");
        int price = seatPrice != null ? seatPrice.intValue() : 0;

        // DB 저장할 ticket_code 속성값 (날짜/시간/UUID)
        java.time.ZoneId KST = java.time.ZoneId.of("Asia/Seoul");
        java.time.LocalDateTime now = java.time.LocalDateTime.now(KST);

        String date = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = now.format(java.time.format.DateTimeFormatter.ofPattern("HHmm"));
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // ticket_code_DB 저장 문자열
        String payload = String.format("%s_%s_%s", date, time, uuid);

        // DB 저장
        TicketsDto dto = new TicketsDto();
        dto.setRno(rno);
        dto.setTicket_code(payload);
        dto.setPrice(price);
        dto.setValid(true);

        ticketsMapper.ticketWrite(dto);
        return true;
    }//func end

    // 프론트 QR코드 생성하는 payload 조회 (qrcode.react 렌더링용)
    @Transactional(readOnly = true) // (readOnly = true) 읽기전용, 쓰기X
    public List<String> findPayloads(int mno) {
        return ticketsMapper.findPayloads(mno);
    }//func end

    // QR코드 (이름/구역/좌석/사용여부)정보 출력
    @Transactional(readOnly = true) // 읽기전용
    public Map<String,Object>findPayloadsInfo(String ticket_code){
        Map<String,Object> info = ticketsMapper.findPayloadsInfo(ticket_code);
        if(info==null)return null; // ticket_code 속성값 null 여부 확인

        boolean valid = Boolean.TRUE.equals(info.get("valid")); //valid 여부 확인
        String validText = valid ? "사용 가능" : " 사용 불가능"; // true,false 대신 사용가능,사용불가능 변환

        String name = Objects.toString(info.get("name"),"");
        String zone = Objects.toString(info.get("zone"),"");
        String seat = Objects.toString(info.get("seat"),"");

        // 순서보장 및 한글 변환
        Map<String,Object> infoPatch = new LinkedHashMap<>();
        infoPatch.put("이름",name);
        infoPatch.put("구역",zone);
        infoPatch.put("좌석",seat);
        infoPatch.put("사용여부",validText);

        return infoPatch;
    }//func end
}//class end
