package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import phoenix.model.dto.TicketsDto;
import phoenix.model.mapper.TicketsMapper;
import org.springframework.stereotype.Service;
import phoenix.util.TicketsQR;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TicketsService {
    private final TicketsMapper ticketsMapper;
    private final TicketsQR ticketsQR;

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

        // QR 스캔 정보
        String name = Objects.toString(info.get("mname"),"");
        String zone = Objects.toString(info.get("zname"),"");
        String seat = Objects.toString(info.get("seat_no"),"");

        // 사용여부 표기
        boolean valid = true;
        String validCheck = valid ? "사용 가능" : "사용 불가능";

        // 한글변환
        String payload = String.format(
                "이름 : %s \n 구역 : %s \n 좌석 : %s \n 사용여부: %s",
                name,zone,seat,validCheck
        );

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
