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

    //예약 rno가 'reserved' 일 때만 QR을 생성하여 tickets에 INSERT
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

        // DB 저장용 (날짜/시간/UUID)
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

    // 회원별 payload 조회
    @Transactional(readOnly = true)
    public List<String> getPayloadsByMno(int mno) {
        return ticketsMapper.findPayloads(mno);
    }//func end

}//class end
