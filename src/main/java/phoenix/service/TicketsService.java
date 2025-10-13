package phoenix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
    private final TicketsQR ticketsQR; // @Component 로 등록되어 있다고 가정


     //예약 rno가 'reserved' 일 때만 QR을 생성하여 tickets에 INSERT
        @Transactional
        public boolean issueIfReserved(int rno) {
        // 예약확인
        Map<String, Object> info = ticketsMapper.ticketPrint(rno);
        if (info == null) return false;

        String status = String.valueOf(info.get("reservation_status"));
        if (!"reserved".equalsIgnoreCase(status)) return false; // 예약상태가 reserved가 아닐 경우 종료


        // 가격조회
        Number seatPrice = (Number) info.get("seat_price");
        int price = seatPrice != null ? seatPrice.intValue() : 0;

        // 이미지생성 (QR → Base64)
        String base64 = qrBase64(String.valueOf(rno));

        // DB저장
        TicketsDto dto = new TicketsDto();
        dto.setRno(rno);
        dto.setTicket_code(base64);
        dto.setPrice(price);
        dto.setValid(true);

        ticketsMapper.insertTicket(dto);
        return true;
    }

    // QR PNG 바이트 생성 후 Base64로 반환
    private String qrBase64(String payloadText) {
        byte[] png = ticketsQR.TicketQrCode(Map.of("text", payloadText));
        return java.util.Base64.getEncoder().encodeToString(png);
    }

}//func end
