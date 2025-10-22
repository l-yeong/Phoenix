package phoenix.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import phoenix.model.dto.SeatDto;
import phoenix.model.dto.SeatsDto;
import phoenix.service.MembersService;
import phoenix.service.SeatLockService;
import phoenix.service.SeatsService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [SeatController]
 * - "좌석 선택/해제/확정(결제)" HTTP API
 * - SeatLockService와 1:1 매핑, 프론트 연동에 최적화
 * - 모든 엔드포인트는 JSON 요청/응답을 기준으로 설계

 *  1) POST /seat/select   : 단일 좌석 선택 (락 + hold 등록)
 *  2) POST /seat/release  : 단일 좌석 해제 (hold 제거)
 *  3) POST /seat/confirm  : 여러 좌석 결제 확정 (SOLD 등록)
 *  4) POST /seat/map       : 좌석 상태 맵 조회
 */

@RestController
@RequestMapping("/seat")
@RequiredArgsConstructor
public class SeatsController {  // class start


    // 의존성 주입
    private final SeatLockService seatService;
    private final MembersService membersService;
    private final SeatsService sService;

    /** (부분) 상태 조회 — 화면에 보이는 좌석만 물어본다(네트워크/서버 비용 최소화) */
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestBody SeatsDto.StatusReq req) {
        int mno = membersService.getLoginMember().getMno();
        var statusMap = seatService.getSeatStatusFor(
                req.getGno(), mno, req.getSeats().stream().map(SeatsDto.SeatRef::getSno).toList()
        );
        return ResponseEntity.ok(Map.of("statusBySno", statusMap));
    }

    /** 좌석 선택(락 시도 → 임시 보유) */
    @PostMapping("/select")
    public ResponseEntity<Map<String, Object>> select(@RequestBody SeatsDto.SingleSeatReq req) throws InterruptedException {
        int mno = membersService.getLoginMember().getMno();
        int code = seatService.tryLockSeat(mno, req.getGno(), req.getZno(), req.getSno());
        return ResponseEntity.ok(Map.of("ok", code == 1, "code", code));
    }

    /** 좌석 해제(내 임시 보유 해제) */
    @PostMapping("/release")
    public ResponseEntity<Map<String, Object>> release(@RequestBody SeatsDto.SingleSeatReq req) {
        int mno = membersService.getLoginMember().getMno();
        boolean ok = seatService.releaseSeat(mno, req.getGno(), req.getZno(), req.getSno());
        return ResponseEntity.ok(Map.of("ok", ok));
    }
    // ---------- 결제(초기: Redis만 반영) ----------
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@RequestBody SeatsDto.ConfirmReq req) {
        int mno = membersService.getLoginMember().getMno();
        StringBuilder reason = new StringBuilder();
        boolean ok = seatService.confirmSeats(mno, req.getGno(), req.getSnos(), reason);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", ok);
        if (!ok) body.put("reason", reason.toString());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/print")
    public ResponseEntity<?> seatPrint(@RequestParam int rno){
        List<SeatDto> result = sService.seatPrint(rno);
        System.out.println("result = " + result);
        return ResponseEntity.ok(result);
    }// func end
}   // class end
