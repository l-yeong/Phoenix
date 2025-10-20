package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.service.TicketsService;
import java.util.List;
import java.util.Map;


@RequestMapping("/tickets")
@RestController
@RequiredArgsConstructor
public class TicketsController {
    private final TicketsService ticketsService;


    /**
     * 예약 rno가 'reserved' 일 때만 QR 문자열 생성하여 tickets 테이블에 저장
     * 예: POST /tickets/write?rno=40001
     */
    @PostMapping("/write")
    public ResponseEntity<Boolean>ticketWrite(@RequestParam int rno) {
        boolean result = ticketsService.ticketWrite(rno);
        return ResponseEntity.ok(result);
    }//func end

    /**
     * 회원별 payload(QR 이미지 URL) 조회
     * 예: GET /tickets/print?mno=20001
     */
    @GetMapping("/print")
    public ResponseEntity<List<String>>findPayloads(@RequestParam int mno) {
        List<String> result = ticketsService.findPayloads(mno);
        return ResponseEntity.ok(result);
    }//func end

    /**
     * 지난 경기 티켓 무효화 (valid = false)
     * 예: POST /tickets/Nullify
     */
    @PostMapping("/nullify")
    public ResponseEntity<?> ticketNullify() {
        int result = ticketsService.ticketNullifyCsv();
        return ResponseEntity.ok(result);
    }//func end

    /**
     * 지난 경기 티켓 QR 코드 삭제 (ticket_code = NULL)
     * 예: POST /tickets/delete
     */
    @PostMapping("/delete")
    public ResponseEntity<?> ticketDelete() {
        int result = ticketsService.ticketDeleteCsv();
        return ResponseEntity.ok(result);
    }//func end

}//func end
