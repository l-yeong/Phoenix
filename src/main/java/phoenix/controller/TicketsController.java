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

    // 예약이 reserved일 때만 발급(SELECT+INSERT를 서비스 한 메서드에서 처리)
    @PostMapping("/write")
    public ResponseEntity<Void> issue(@AuthenticationPrincipal @RequestParam int rno) {
        boolean created = ticketsService.ticketWrite(rno);
        return created ? ResponseEntity.ok().build()
                : ResponseEntity.noContent().build(); // reserved 아님
    }

}//func end
