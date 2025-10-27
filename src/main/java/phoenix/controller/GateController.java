// src/main/java/phoenix/controller/GateController.java
package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import phoenix.model.dto.GateDto;
import phoenix.service.GateService;
import phoenix.service.MembersService;

import java.util.HashMap;
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


    // === [3] 상태 조회: 쿼리로 gno 받기 (선택 사용)
    @GetMapping("/status")
    public ResponseEntity<GateDto.StatusResponse> status(@RequestParam int gno) {
        return ResponseEntity.ok(new GateDto.StatusResponse(
                gateService.waitingCount(gno),
                gateService.availablePermits(gno)
        ));
    }

    // === [5] 세션 alive + TTL + 메타 ===
    @GetMapping("/check/{gno}")
    public ResponseEntity<Map<String, Object>> check(@PathVariable int gno) {
        int mno = membersService.getLoginMember().getMno();
        boolean ready = gateService.isEntered(mno, gno);
        long ttlMs = ready ? gateService.remainTtlMillis(mno, gno) : 0L;
        Map<String, Object> body = new HashMap<>();
        body.put("ready", ready);
        body.put("ttlSec", (int)Math.ceil(ttlMs / 1000.0));
        body.put("permits", gateService.availablePermits(gno));
        body.put("waiting", gateService.waitingCount(gno));
        return ResponseEntity.ok(body);
    }

    // === [pos] 내 순번 ===
    @GetMapping("/position/{gno}")
    public ResponseEntity<Map<String, Object>> position(@PathVariable int gno) {
        int mno = membersService.getLoginMember().getMno();
        Integer pos = gateService.positionOf(mno, gno);
        return ResponseEntity.ok(Map.of("position", pos == null ? -1 : pos));
    }

    // GateController
    @PostMapping("/leave")
    public ResponseEntity<GateDto.LeaveResponse> leave(
            @RequestParam(value = "gno", required = false) Integer gnoParam,
            @RequestBody(required = false) Integer gnoBody
    ) {
        int gno = (gnoParam != null ? gnoParam : (gnoBody != null ? gnoBody : 0));
        if (gno <= 0) return ResponseEntity.badRequest().body(new GateDto.LeaveResponse(false));

        int mno = membersService.getLoginMember().getMno();
        boolean ok = gateService.leave(mno, gno);
        return ResponseEntity.ok(new GateDto.LeaveResponse(ok));
    }
}
