package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import phoenix.model.dto.SeatsDto;
import phoenix.service.SeatLockService;

import java.util.Map;

/**
 * ================================================================
 * [SeatController]
 * - "좌석 선택/해제/확정(결제)" HTTP API
 * - SeatLockService와 1:1 매핑, 프론트 연동에 최적화
 * - 모든 엔드포인트는 JSON 요청/응답을 기준으로 설계
 * ================================================================
 *
 * 엔드포인트 요약
 *  1) POST /api/seat/select   : 단일 좌석 선택 (락 + hold 등록)
 *  2) POST /api/seat/release  : 단일 좌석 해제 (hold 제거)
 *  3) POST /api/seat/confirm  : 여러 좌석 결제 확정 (SOLD 등록)
 *  4) (선택) POST /api/seat/map : 좌석 상태 맵 조회
 */
@RestController
@RequestMapping("/api/seat")
@RequiredArgsConstructor
public class SeatsController {

    private final SeatLockService seatService;

    // ------------------------------------------------------------
    // 1) 단일 좌석 선택
    // ------------------------------------------------------------
    @PostMapping("/select")
    public ResponseEntity<SeatsDto.SelectResponse> select(@RequestBody SeatsDto.SelectRequest req)
            throws InterruptedException {

        // SeatLockService 시그니처에 맞춰 (userId, showId, seatId) 전달
        boolean ok = seatService.tryLockSeat(req.getUserId(), req.getShowId(), req.getSeatId());

        // 사용자 친화 메시지 구성
        String msg = ok ? "locked" : "already held/sold or session missing or limit exceeded";
        return ResponseEntity.ok(new SeatsDto.SelectResponse(ok, msg));
    }

    // ------------------------------------------------------------
    // 2) 좌석 해제
    // ------------------------------------------------------------
    @PostMapping("/release")
    public ResponseEntity<SeatsDto.ReleaseResponse> release(@RequestBody SeatsDto.ReleaseRequest req) {
        boolean ok = seatService.releaseSeat(req.getUserId(), req.getSeatId());
        return ResponseEntity.ok(new SeatsDto.ReleaseResponse(ok));
    }

    // ------------------------------------------------------------
    // 3) 좌석 확정(결제)
    // ------------------------------------------------------------
    @PostMapping("/confirm")
    public ResponseEntity<SeatsDto.ConfirmResponse> confirm(@RequestBody SeatsDto.ConfirmRequest req) {
        StringBuilder reason = new StringBuilder();

        // SeatLockService 시그니처 주의!
        // confirmSeats(userId, seatIds, showId, failReason)
        boolean ok = seatService.confirmSeats(req.getUserId(), req.getSeatIds(), req.getShowId(), reason);

        return ResponseEntity.ok(
                new SeatsDto.ConfirmResponse(ok, ok ? "confirmed" : reason.toString())
        );
    }

    // ------------------------------------------------------------
    // 4) (선택) 좌석 상태 조회
    // - SeatLockService에 map 기능을 열어두면 활성화
    // ------------------------------------------------------------
    // @PostMapping("/map")
    // public ResponseEntity<SeatsDto.MapResponse> map(
    //         @RequestParam String userId,
    //         @RequestBody(required = false) SeatsDto.MapRequest req) {
    //
    //     var seatIds = (req == null) ? null : req.getSeatIds();
    //     Map<String, String> status = seatService.getSeatStatusMap(seatIds, userId);
    //     return ResponseEntity.ok(new SeatsDto.MapResponse(status));
    // }
}
