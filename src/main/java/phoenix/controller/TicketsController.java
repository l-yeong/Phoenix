package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.service.TicketsService;
import java.util.List;


@RequestMapping("/ticket")
@RestController
@RequiredArgsConstructor
public class TicketsController {
    private final TicketsService ticketsService;

    // (기존) 발급: reserved일 때 INSERT
    @PostMapping("/write")
    public ResponseEntity<?> ticketWrite(@RequestParam int rno) {
        boolean ok = ticketsService.ticketWrite(rno);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.noContent().build();
    }//func end

    // 프론트가 쓰는 payload 조회 (qrcode.react 렌더링용)
    @GetMapping("/payloads")
    public ResponseEntity<List<String>> payloads(@RequestParam int mno) {
        List<String> list = ticketsService.getPayloadsByMno(mno);
        return ResponseEntity.ok(list);
    }//func end

}//func end
