// src/main/java/phoenix/controller/GateController.java
package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import phoenix.model.dto.GateDto;
import phoenix.service.GateService;
import phoenix.service.MembersService;

import java.util.Map;

@RestController
@RequestMapping("/gate")
@RequiredArgsConstructor
public class GateController {

    private final GateService gateService;
    private final MembersService membersService;

    // === [1] 대기열 등록: 프론트가 '숫자 gno'를 JSON 바디로 보냄 ===
    @PostMapping(value = "/enqueue", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GateDto.EnqueueResponse> enqueue(@RequestBody int gno) {
        int mno = membersService.getLoginMember().getMno();
        var res = gateService.enqueue(mno, gno);
        return ResponseEntity.ok(new GateDto.EnqueueResponse(res.queued(), res.waiting()));
    }

    // === [2] 퇴장: 바디/쿼리 어느 쪽이든 gno 수용 (before unload용 keepalive POST) ===
    @PostMapping("/leave")
    public ResponseEntity<GateDto.LeaveResponse> leave(@RequestBody int gno) {

        int mno = membersService.getLoginMember().getMno();
        boolean ok = gateService.leave(mno, gno);
        return ResponseEntity.ok(new GateDto.LeaveResponse(ok));

    }

    // === [3] 상태 조회: 쿼리로 gno 받기 (선택 사용)
    @GetMapping("/status")
    public ResponseEntity<GateDto.StatusResponse> status(@RequestParam int gno) {
        return ResponseEntity.ok(new GateDto.StatusResponse(
                gateService.waitingCount(gno),
                gateService.availablePermits(gno)
        ));
    }

    // === [5] 세션 alive 확인: 패스 파라미터 방식(/check/{gno})을 지원 (프론트 호출과 1:1 매칭) ===
    @GetMapping("/check/{gno}")
    public ResponseEntity<Map<String, Boolean>> check(@PathVariable int gno) {
        int mno = membersService.getLoginMember().getMno();
        boolean ready = gateService.isEntered(mno, gno);
        return ResponseEntity.ok(Map.of("ready", ready));
    }

    // === [6] 포지션 조회: 패스 파라미터 방식(/position/{gno}) 지원 ===
    @GetMapping("/position/{gno}")
    public ResponseEntity<Map<String, Integer>> position(@PathVariable int gno) {
        int mno = membersService.getLoginMember().getMno();
        Integer pos = gateService.positionOf(mno, gno);
        return ResponseEntity.ok(Map.of("position", pos == null ? -1 : pos));
    }
}
