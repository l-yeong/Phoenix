package phoenix.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.service.MembersService;
import phoenix.service.TicketsService;

import java.net.URI;
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
    public ResponseEntity<Boolean> ticketWrite(@RequestParam int rno) {
        boolean result = ticketsService.ticketWrite(rno);
        return ResponseEntity.ok(result);
    }//func end

    /**
     * 회원별 payload(QR 이미지 URL) 조회
     * 예: GET /tickets/print
     */
    @GetMapping("/print")
    public ResponseEntity<?> findPayloads(@RequestParam(name = "rno", required = false) Integer rno) {
        MembersDto loginMno = membersService.getLoginMember();

        // 로그인 여부 확인
        if (loginMno == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        //  rno 값 검증
        if (rno == null) {
            return ResponseEntity.badRequest().body("rno 파라미터가 필요합니다.");
        }

        int mno = loginMno.getMno();
        List<Map<String, Object>> result = ticketsService.findPayloads(mno, rno);

        return ResponseEntity.ok(result);
    }//func end

    /**
     * QR 링크로 진입: /tickets/qr?qr={uuid}
     * - JSON 바로 반환 (프론트 새 QR 전용 컴포넌트가 axios로 호출)
     */
    @GetMapping("/qr")
    public ResponseEntity<?> ticketUrlUuid(@RequestParam("qr") String uuid) {
        int info = ticketsService.ticketUrlUuid(uuid);
        return ResponseEntity.ok(info);
    }//func end


    @GetMapping("/qrInfo")
    public ResponseEntity<?> ticketUuidInfo(@RequestParam("qr") String uuid) {
        Map<String,Object> info = ticketsService.ticketUuidInfo(uuid);
        if (info == null || info.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "티켓을 찾을 수 없습니다.", "uuid", uuid));
        }//if end
        return ResponseEntity.ok(info);
    }//func end
}//class end

