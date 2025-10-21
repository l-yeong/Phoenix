package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.GameDto;
import phoenix.service.GameService;
import java.util.*;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    // ✅ 전체 경기 목록 조회
    @GetMapping("/all")
    public ResponseEntity<List<GameDto>> all() {
        return ResponseEntity.ok(gameService.findAll());
    }

    // ✅ 단일 경기 예매 가능 여부 (최종 확인)
    @GetMapping("/check/{gno}")
    public ResponseEntity<Map<String, Object>> check(@PathVariable int gno) {
        boolean ok = gameService.isReservable(gno);
        return ResponseEntity.ok(Map.of(
                "ok", ok,
                "msg", ok ? "예매 가능 경기입니다." : "예매 불가능한 경기입니다."
        ));
    }
}