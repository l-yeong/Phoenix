package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.mapper.SeatsMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/zone")
@RequiredArgsConstructor
public class ZoneController {

    private final SeatsMapper seatsMapper;

    /**
     * 특정 존의 좌석 목록 (sno, seatName)
     * - 프론트: 이 결과로 버튼을 만들고, sno(PK)만 서버로 보냄
     * - 모든 존의 좌석 수/패턴이 동일 → 프론트는 공용 템플릿 사용 가능
     */
    @GetMapping("/{zno}/seats")
    public ResponseEntity<?> seatsByZone(@PathVariable int zno) {
        // zno 유효성 (존 존재 여부)
        if (!seatsMapper.existsZone(zno)) {
            return ResponseEntity.status(404).body(Map.of(
                    "ok", false,
                    "error", "ZONE_NOT_FOUND",
                    "zno", zno
            ));
        }

        List<Map<String, Object>> rows = seatsMapper.findSeatsByZone(zno); // [{sno, seatName}]
        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("zno", zno);
        body.put("count", rows.size());
        body.put("seats", rows);
        return ResponseEntity.ok(body);
    }
}
