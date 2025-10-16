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
    private final FileService fileService;

    //예약 rno가 'reserved' 일 때만 QR 문자열 생성하여 tickets에 테이블에 저장
    @Transactional
    public boolean ticketWrite(int rno) {
        //예약 정보 조회
        Map<String, Object> info = ticketsMapper.ticketPrint(rno);
        if (info == null) return false;

        String status = String.valueOf(info.get("reservation_status"));
        if (!"reserved".equalsIgnoreCase(status)) return false;

        // 기존 QR 존재 여부 확인
        String existingCode = ticketsMapper.findTicketCodeByRno(rno);
        if (existingCode != null && !existingCode.isEmpty()) {
            return false;
        }//if end

        // 가격 조회
        Number seatPrice = (Number) info.get("seat_price");
        int price = seatPrice != null ? seatPrice.intValue() : 0;

        // QR 코드용 고정 문자열
        java.time.ZoneId KST = java.time.ZoneId.of("Asia/Seoul");
        String date = java.time.LocalDate.now(KST)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String payload = String.format("%s_%s", date, uuid);

        // QR 스캔시 정보 출력
        String name = String.valueOf(info.getOrDefault("mname",""));
        String zone = String.valueOf(info.getOrDefault("zname",""));
        String seat = String.valueOf(info.getOrDefault("seat_no",""));
        String validText = "사용가능";

        Map<String,Object>qrPayload=new HashMap<>();
        qrPayload.put("이름",name);
        qrPayload.put("구역",zone);
        qrPayload.put("좌석",seat);
        qrPayload.put("사용여부",validText);
        String qrText;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            qrText = om.writeValueAsString(qrPayload);
        } catch (Exception e) {
            return false;
        }

        // QR 이미지 파일 생성 및 저장
        String imagePath = fileService.saveQRImg(qrText);

        //DB저장
        TicketsDto dto = new TicketsDto();
        dto.setRno(rno);
        dto.setTicket_code(imagePath);
        dto.setPrice(price);
        dto.setValid(true);

        ticketsMapper.ticketWrite(dto);
        return true;
    }//func end

    // 회원별 payload(QR 이미지 URL) 조회
    @Transactional(readOnly = true)
    public List<String> findPayloads(int mno) {
        return ticketsMapper.findPayloads(mno);
    }//func end

    // QR 상세 정보 조회
    @Transactional(readOnly = true)
    public Map<String, Object> findPayloadsInfo(String ticket_code) {
        Map<String, Object> info = ticketsMapper.findPayloadsInfo(ticket_code);
        if (info == null) return null;

        boolean valid = Boolean.TRUE.equals(info.get("valid"));
        String validText = valid ? "사용 가능" : "사용 불가능";

        String name = Objects.toString(info.get("name"), "");
        String zone = Objects.toString(info.get("zone"), "");
        String seat = Objects.toString(info.get("seat"), "");

        Map<String, Object> infoPatch = new LinkedHashMap<>();
        infoPatch.put("이름", name);
        infoPatch.put("구역", zone);
        infoPatch.put("좌석", seat);
        infoPatch.put("사용여부", validText);

        return infoPatch;
    }//func end


}//class end
