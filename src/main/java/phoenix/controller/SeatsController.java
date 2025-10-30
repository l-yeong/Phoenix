package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import phoenix.model.dto.AutoSelectDto;
import phoenix.model.dto.SeatDto;
import phoenix.model.dto.SeatsDto;
import phoenix.service.AutoSeatsService;
import phoenix.service.MembersService;
import phoenix.service.SeatLockService;
import phoenix.util.RedisKeys;

import java.util.*;

/**
 * [SeatController]
 * - 일반 예매(수동/자동) 좌석 선택/해제/확정 + 상태 맵 + 스냅샷
 *
 *  1) POST /seat/select       : 단일 좌석 선택 (락 + hold 등록)
 *  2) POST /seat/release      : 단일 좌석 해제 (hold 제거)
 *  3) POST /seat/confirm      : 여러 좌석 결제 확정 (SOLD 등록)
 *  4) POST /seat/status       : 좌석 상태 맵 조회(부분)
 *  5) GET  /seat/held         : [🆕] 내 임시보유 좌석 스냅샷
 *  6) POST /seat/confirm/all  : [🆕] 내 임시보유 전체 확정
 *  7) POST /seat/auto         : 자동예매(임시홀드까지 수행, 일반 예매용)
 *  8) GET  /seat/print        : 예매 rno 기준 동일 존 좌석 리스트
 */
@RestController
@RequestMapping("/seat")
@RequiredArgsConstructor
public class SeatsController {

    private final SeatLockService seatService;
    private final MembersService membersService;
    private final AutoSeatsService autoSeatsService;
    private final RedissonClient redisson;

    /** 좌석 선택(락 시도 → 임시 보유) */
    @PostMapping("/select")
    public ResponseEntity<Map<String, Object>> select(@RequestBody SeatsDto.SingleSeatReq req) throws InterruptedException {
        int mno = membersService.getLoginMember().getMno();
        int code = seatService.tryLockSeat(mno, req.getGno(), req.getZno(), req.getSno());
        int remain = seatService.remainingSelectableSeats(mno, req.getGno()); // 🆕 잔여 매수
        return ResponseEntity.ok(Map.of(
                "ok", code == 1,
                "code", code,
                "remain", remain
        ));
    }

    /** (부분) 상태 조회 — 화면에 보이는 좌석만 요청 */
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestBody SeatsDto.StatusReq req) {
        int mno = membersService.getLoginMember().getMno();

        // seats null-safe + sno 추출
        List<Integer> snos = (req.getSeats() == null ? List.<SeatsDto.SeatRef>of() : req.getSeats())
                .stream().map(SeatsDto.SeatRef::getSno).toList();

        Map<Integer, String> statusMap = seatService.getSeatStatusFor(req.getGno(), mno, snos);
        int remain = seatService.remainingSelectableSeats(mno, req.getGno()); // 🆕 잔여 매수

        return ResponseEntity.ok(Map.of(
                "statusBySno", statusMap,
                "remain", remain
        ));
    }

    /** 좌석 해제(내 임시 보유 해제) */
    @PostMapping("/release")
    public ResponseEntity<Map<String, Object>> release(@RequestBody SeatsDto.SingleSeatReq req) {
        int mno = membersService.getLoginMember().getMno();
        boolean ok = seatService.releaseSeat(mno, req.getGno(), req.getZno(), req.getSno());
        int remain = seatService.remainingSelectableSeats(mno, req.getGno()); // 🆕 잔여 매수
        return ResponseEntity.ok(Map.of(
                "ok", ok,
                "remain", remain
        ));
    }

    /** 결제 확정(선택 좌석 목록) */
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

    // ==============================
    // 🆕 내 임시보유 좌석 스냅샷
    // ==============================
    @GetMapping("/held")
    public ResponseEntity<Map<String, Object>> held(@RequestParam int gno) {
        int mno = membersService.getLoginMember().getMno();
        Set<Integer> heldSnos = seatService.getUserHoldSnapshot(mno, gno); // ← 스냅샷 사용
        int remain = seatService.remainingSelectableSeats(mno, gno);       // 함께 내려주면 프론트 편함
        return ResponseEntity.ok(Map.of(
                "heldSnos", heldSnos,
                "count", heldSnos.size(),
                "remain", remain
        ));
    }

    // ==============================
    // 🆕 내 임시보유 전체 확정
    // ==============================
    @PostMapping("/confirm/all")
    public ResponseEntity<Map<String, Object>> confirmAll(@RequestBody Map<String, Integer> req) {
        int mno = membersService.getLoginMember().getMno();
        int gno = req.get("gno");

        Set<Integer> held = seatService.getUserHoldSnapshot(mno, gno);
        if (held.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "ok", false,
                    "count", 0,
                    "reason", "NO_HELD_SEATS"
            ));
        }

        List<Integer> snos = new ArrayList<>(held);
        StringBuilder reason = new StringBuilder();
        boolean ok = seatService.confirmSeats(mno, gno, snos, reason);

        return ResponseEntity.ok(Map.of(
                "ok", ok,
                "count", snos.size(),
                "reason", reason.toString()
        ));
    }

    /** 자동예매(임시홀드까지 수행, 일반 예매용) */
    @PostMapping("/auto")
    public ResponseEntity<AutoSelectDto.AutoSelectRes> auto(@RequestBody AutoSelectDto.AutoSelectReq req) {
        int mno = membersService.getLoginMember().getMno();
        AutoSelectDto.AutoSelectRes res = autoSeatsService.autoAssignAndHold(mno, req);
        return ResponseEntity.ok(res);
    }

    /** rno 기준 동일 존 좌석 프린트(기존 유지) */
    @GetMapping("/print")
    public ResponseEntity<?> seatPrint(@RequestParam int rno){
        List<SeatDto> result = seatService.seatPrint(rno);
        return ResponseEntity.ok(result);
    }
    @GetMapping("/check/senior")
    public Map<String, Boolean> checkSenior(@RequestParam int gno) {
        int mno = membersService.getLoginMember().getMno();
        boolean hasSenior = redisson.getAtomicLong(RedisKeys.keySeniorBooked(mno, gno)).get() > 0;
        return Map.of("senior", hasSenior);
    }
}
