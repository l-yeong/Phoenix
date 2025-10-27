package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import phoenix.service.SeatCsvService;

@RestController
@RequestMapping("/zone")
@RequiredArgsConstructor
public class ZoneController {

    private final SeatCsvService seatCsvService;  // CSV 메타 참조 의존성 주입

    /**
     * 특정 존의 좌석 목록 (sno, seatName)
     * - 프론트: 이 결과로 버튼을 만들고, sno(PK)만 서버로 보냄
     * - 모든 존의 좌석 수/패턴이 동일 → 프론트는 공용 템플릿 사용 가능
     * - ★ DB 대신 CSV 정적파일(seats.csv) 기반
     */
    @GetMapping("/{zno}/seats")
    public ResponseEntity<?> seatsByZone(@PathVariable int zno) {
        // zno 유효성 (존 존재 여부) - CSV 기반
        if (!seatCsvService.existsZone(zno)) {
            return ResponseEntity.status(404).body(Map.of(
                    "ok", false,
                    "error", "ZONE_NOT_FOUND",
                    "zno", zno
            ));
        }

        // CSV에서 좌석 목록 가져오기 + 정렬
        var metas = seatCsvService.getSeatsByZoneSorted(zno);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SeatCsvService.SeatCsvDto m : metas) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sno", Integer.valueOf(m.getSno()));
            item.put("seatName", m.getSeatName());
            rows.add(item);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("zno", zno);
        body.put("count", rows.size());
        body.put("seats", rows);
        return ResponseEntity.ok(body);
    }
}
