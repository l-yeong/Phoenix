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
     * 티켓 발급
     * - 예약 상태가 'reserved'인 경우에만 QR 코드를 생성하여 tickets 테이블에 저장
     * - 예약 상태가 유효하지 않거나 이미 발급된 경우 false를 반환
     *
     * @param rno 예매 고유번호
     * @return ResponseEntity<Boolean>
     *         true  → 발급 성공
     *         false → 중복 또는 부적합 상태로 인한 실패
     *
     * POST /tickets/write?rno=40001
     */
    //@PostMapping("/write")
    //public ResponseEntity<Boolean> ticketWrite(@RequestParam int rno) {
    //    boolean result = ticketsService.ticketWrite(rno);
    //    return ResponseEntity.ok(result);
    //}//func end

    /**
     * 회원별 티켓 payload(QR 이미지 경로) 조회

     * - 로그인된 회원의 mno를 기준으로, 특정 예매번호(rno)에 대한 티켓 정보를 반환합
     * - QR 이미지 URL, 유효 상태, 좌석정보 등을 포함
     * 예외 처리:
     * - 로그인 정보 없음 → 401 (UNAUTHORIZED)
     * - rno 누락 → 400 (BAD_REQUEST)
     *
     * @param rno 예매 고유번호
     * @return ResponseEntity<List<Map<String,Object>>>
     *         회원별 QR payload 목록
     *
     * GET /tickets/print?rno=40001
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
     * QR UUID 기반 티켓 상세 조회
     * <p>
     * - QR 코드(UUID)를 통해 티켓 상세정보(회원명, 구역명, 좌석명, 유효상태 등)를 조회
     * - 존재하지 않는 UUID일 경우 404 반환.
     *
     * @param uuid 티켓 UUID
     * @return ResponseEntity<Map<String,Object>>
     *         티켓 상세 정보 또는 오류 메시지
     *
     * GET /tickets/qrInfo?qr=abc123
     */
    @GetMapping("/qrInfo")
    public ResponseEntity<?> ticketUuidInfo(@RequestParam("qr") String uuid) {
        Map<String,Object> info = ticketsService.ticketUuidInfo(uuid);
        if (info == null || info.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "티켓을 찾을 수 없습니다.", "uuid", uuid));
        }//if end
        return ResponseEntity.ok(info);
    }//func end


    /**
     * QR 스캔 처리
     * <p>
     * - QR 코드(UUID)를 스캔하여 유효 여부를 검증하고, 사용 처리(valid=1→0)
     * - 이미 사용된 티켓일 경우 실패 메시지를 반환
     *
     * @param uuid 티켓 UUID
     * @return ResponseEntity<Map<String,Object>>
     *         { success: true/false, message: "..." }
     *
     * GET /tickets/qr?qr=abc123
     */
    @GetMapping("/qr")
    public ResponseEntity<?> qrScan(@RequestParam("qr")String uuid){
        MembersDto loginMember = membersService.getLoginMember();
        if(loginMember == null ){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        if(!"admin".equalsIgnoreCase(loginMember.getMid())){ // equalsIgnoreCase = 대소문자 구분없음,ex)Admin,admin
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("관리자만 접근 가능합니다.");
        }//func end

        Map<String, Object> result = ticketsService.qrScan(uuid);
        return ResponseEntity.ok(result);
    }//func end

    /**
     * 관리자용 QR 사용 로그 조회
     * - 전체 티켓의 사용 이력(회원명, 연락처, 구역, 좌석, 유효상태 등)을 조회합니다.
     * - 관리자 권한이 필요한 엔드포인트입니다.
     *
     * @return ResponseEntity<List<Map<String,Object>>>
     *         전체 QR 사용 로그 목록
     *
     * GET /tickets/ticketLog
     */
    @GetMapping("/ticketLog")
    public ResponseEntity<?>adminScanLog(){
        MembersDto loginAdmin = membersService.getLoginMember();
        if(loginAdmin ==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }//if end
        if(!"admin".equalsIgnoreCase(loginAdmin.getMid())){ // equalsIgnoreCase = 대소문자 구분없음,ex)Admin,admin
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("관리자만 접근 가능합니다.");
        }//func end
        List<Map<String,Object>> result = ticketsService.adminScanLog();
        return ResponseEntity.ok(result);
    }//func end
}//class end

