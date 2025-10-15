package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.service.TicketsService;
import java.util.List;
import java.util.Map;


@RequestMapping("/ticket")
@RestController
@RequiredArgsConstructor
public class TicketsController {
    private final TicketsService ticketsService;

    //예약 rno가 'reserved' 일 때만 QR 문자열 생성하여 tickets에 테이블에 저장
    @PostMapping("/write")
    public ResponseEntity<?> ticketWrite(@RequestParam int rno) {
        boolean result = ticketsService.ticketWrite(rno);
        return ResponseEntity.ok(result);
    }//func end

    // 프론트 QR코드 생성하는 payload 조회 (qrcode.react 렌더링용)
    @GetMapping("/payloads")
    public ResponseEntity<List<String>> payloads(@RequestParam int mno) {
        List<String> result = ticketsService.findPayloads(mno);
        return ResponseEntity.ok(result);
    }//func end

    // QR코드 (이름/구역/좌석/사용여부)정보 출력
    @GetMapping("/payloads/info")
    public ResponseEntity<?>findPayloadsInfo(@RequestParam String ticket_code){
        Map<String,Object> result = ticketsService.findPayloadsInfo(ticket_code);
        return ResponseEntity.ok(result);
    }//func end

}//func end
