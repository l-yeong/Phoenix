package phoenix.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.service.MembersService;
import phoenix.service.TicketsService;
import java.util.List;
import java.util.Map;


@RequestMapping("/tickets")
@RestController
@RequiredArgsConstructor
public class TicketsController {
    private final TicketsService ticketsService;
    private final MembersService membersService;

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
     * 예: GET /tickets/print
     */
    @GetMapping("/print")
    public ResponseEntity<List<Map<String,Object>>>findPayloads(@AuthenticationPrincipal MembersDto user) {
        //MembersDto login = membersService.getLoginMember(); // 여기서 null일 일 없도록 아래 서비스 수정
        List<Map<String,Object>> result = ticketsService.findPayloads(user.getMno());
        return ResponseEntity.ok(result);
    }//func end

}//class end