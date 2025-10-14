package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.model.dto.TicketsDto;
import phoenix.service.TicketsService;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequestMapping("/ticket")
@RestController
@RequiredArgsConstructor
public class TicketsController {
    private final TicketsService ticketsService;

    // (기존) 발급: reserved일 때 INSERT
    @PostMapping("/write")
    public ResponseEntity<Void> issue(@RequestParam int rno) {
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
