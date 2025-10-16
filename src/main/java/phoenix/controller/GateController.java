package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.GateDto;
import phoenix.service.GateService;

@RestController
@RequestMapping("/api/gate")
@RequiredArgsConstructor
public class GateController {

    private final GateService gateService;

    @PostMapping("/enqueue")
    public ResponseEntity<GateDto.EnqueueResponse> enqueue(@RequestBody GateDto.EnqueueRequest req) {
        var res = gateService.enqueue(req.getUserId(), req.getShowId());
        return ResponseEntity.ok(new GateDto.EnqueueResponse(res.queued(), res.waiting()));
    }

    @PostMapping("/enter")
    public ResponseEntity<GateDto.EnterResponse> enter(@RequestBody GateDto.EnterRequest req) {
        boolean ok = gateService.confirmEnter(req.getUserId(), req.getToken(), req.getShowId());
        return ResponseEntity.ok(new GateDto.EnterResponse(ok, ok ? "entered" : "denied"));
    }

    @PostMapping("/leave")
    public ResponseEntity<GateDto.LeaveResponse> leave(@RequestBody GateDto.LeaveRequest req) {
        boolean ok = gateService.leave(req.getUserId());
        return ResponseEntity.ok(new GateDto.LeaveResponse(ok));
    }

    @GetMapping("/status")
    public ResponseEntity<GateDto.StatusResponse> status() {
        return ResponseEntity.ok(new GateDto.StatusResponse(
                gateService.waitingCount(), gateService.availablePermits()
        ));
    }
}