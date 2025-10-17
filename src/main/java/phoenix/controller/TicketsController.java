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
    public ResponseEntity<Boolean>ticketWrite(@RequestParam int rno) {
        boolean result = ticketsService.ticketWrite(rno);
        return ResponseEntity.ok(result);
    }//func end

    // 회원별 payload(QR 이미지 URL) 조회
    @GetMapping("/print")
    public ResponseEntity<List<String>>findPayloads(@RequestParam int mno) {
        List<String> result = ticketsService.findPayloads(mno);
        return ResponseEntity.ok(result);
    }//func end


}//func end
