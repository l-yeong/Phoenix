package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.GameDto;
import phoenix.service.GameService;
import phoenix.service.PlayerCsvService;
import phoenix.util.ApiResponseUtil;

import java.util.*;
import java.util.stream.Collectors;

// /game 매핑 경로 Security 에서 비회원 열어두게 변경
@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final PlayerCsvService playerCsvService;

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

    // MembersController 에서 옮김
    @GetMapping("/players")
    public ResponseEntity<ApiResponseUtil<?>> getPlayers(){

        List<Map<String , Object>> list = playerCsvService.findAllPlayers().stream()
                .map( p -> {
                    Map<String , Object> map = new HashMap<>();
                    map.put("pno", p.getPno());
                    map.put("name", p.getName());
                    map.put("position", p.getPosition());
                    map.put("teamName", playerCsvService.findTeamName(p.getTeamNo()));
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponseUtil<>(true , "선수 목록 로드 성공" , list));

    } // func e
}