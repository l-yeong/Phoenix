package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import phoenix.model.dto.SeatsDto;
import phoenix.service.SeatLockService;

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

    // 좌석 임시 선택 메소드
    @PostMapping("/select")
    public ResponseEntity<SeatsDto.SelectResponse> select(@RequestBody SeatsDto.SelectRequest req) throws InterruptedException {

        // SeatLockService 시그니처에 맞춰 (userId, showId, seatId) 전달
        int code = seatService.tryLockSeat(req.getUserId(), req.getShowId(), req.getSeatId());

        // 서비스에서 가져온 반환값을 가지고 분기처리
        String msg = switch (code) {
            case 1 -> "lock success";
            case -1 -> "no session";
            case -2 -> "duplicate reservation";
            case -3 -> "already held/sold";
            case -4 -> "limit exceed";
            default -> "error";
        };  // switch end

        // 1이면 성공 나머지 다 실패
        boolean ok =  code == 1;

        // 메세지와 성공여부 보내기
        return ResponseEntity.ok(new SeatsDto.SelectResponse(ok, code , msg));
    }   // func end

    // 좌석 해제 메소드
    // SeatLockService.releaseSeat(userId, seatId, showId) (다회차 안전)
    @PostMapping("/release")
    public ResponseEntity<SeatsDto.ReleaseResponse> release(@RequestBody SeatsDto.ReleaseRequest req) {

        boolean ok = seatService.releaseSeat(req.getUserId(), req.getSeatId(), req.getShowId());

        return ResponseEntity.ok(new SeatsDto.ReleaseResponse(ok));
    }   // func end

    // ------------------------------------------------------------
    // 3) 좌석 확정(결제)
    //   - SeatLockService.confirmSeats(userId, seatIds, showId, failReason)
    // ------------------------------------------------------------
    @PostMapping("/confirm")
    public ResponseEntity<SeatsDto.ConfirmResponse> confirm(@RequestBody SeatsDto.ConfirmRequest req) {
        StringBuilder reason = new StringBuilder();
        boolean ok = seatService.confirmSeats(req.getUserId(), req.getSeatIds(), req.getShowId(), reason);
        // reason 이 서비스에서 StringBuilder 돼서 들어옴.
        return ResponseEntity.ok(
                new SeatsDto.ConfirmResponse(ok, ok ? "confirmed" : reason.toString())
        );
    }   // func end

    // ------------------------------------------------------------
    // 4) (선택) 좌석 상태 조회
    //   - SeatLockService.getSeatStatusMap(...) 열어두면 사용
    // ------------------------------------------------------------
    // @PostMapping("/getMap")
    // public ResponseEntity<SeatsDto.MapResponse> map(
    //         @RequestParam String userId,
    //         @RequestBody(required = false) SeatsDto.MapRequest req) {
    //     var seatIds = (req == null) ? null : req.getSeatIds();
    //     Map<String, String> status = seatService.getSeatStatusMap(seatIds, userId);
    //     return ResponseEntity.ok(new SeatsDto.MapResponse(status));
    // }    // func end

}   // class end
