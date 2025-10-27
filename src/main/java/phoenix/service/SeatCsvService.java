package phoenix.service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 좌석/존 "정적 메타데이터" 로더
 * - DB 대신 CSV로 무결성 검증 수행
 * - 다른 로직(락/hold/confirm/상태조회)은 기존 그대로 둔다
 *
 * CSV 스키마
 *  - zones.csv:   zno,zname
 *  - seats.csv:   sno,zno,seatName,senior
 */
@Service
public class SeatCsvService {

    // === 내부 모델 ===
    @Data
    @AllArgsConstructor
    public static class SeatCsvDto {
        private final int sno;
        private final int zno;
        private final String seatName;
        private final boolean senior;
    }

    private final Set<Integer> allSeatSnos = new HashSet<>();
    private final Map<Integer, Set<Integer>> zoneToSnos = new HashMap<>();
    private final Map<Integer, String> zoneNameByZno = new HashMap<>();
    private final Map<Integer, SeatCsvDto> metaBySno = new HashMap<>();
    private final Map<Integer, List<SeatCsvDto>> seatsListByZone = new HashMap<>();

    @PostConstruct
    public void load() {
        loadZonesCsv("static/zones.csv");
        loadSeatsCsv("static/seats.csv");
        System.out.printf("[SeatCsvService] zones=%d, seats=%d%n",
                zoneNameByZno.size(), allSeatSnos.size());
    }

    // ── 존재/조회 편의 ──────────────────────────────────────────────
    public boolean existsZone(int zno) {
        return zoneNameByZno.containsKey(zno) || seatsListByZone.containsKey(zno);
    }

    public boolean existsSeatBySno(int sno) {
        return allSeatSnos.contains(sno);
    }

    public boolean existsSeatInZone(int zno, int sno) {
        return zoneToSnos.getOrDefault(zno, Collections.emptySet()).contains(sno);
    }

    public String getZoneName(int zno) {
        return zoneNameByZno.getOrDefault(zno, "ZNO " + zno);
    }

    public SeatCsvDto getMeta(int sno) {
        return metaBySno.get(sno);
    }

    public String getSeatName(int sno) {
        SeatCsvDto m = metaBySno.get(sno);
        return m != null ? m.getSeatName() : null;
    }

    public boolean isSeniorSeat(int sno) {
        SeatCsvDto m = metaBySno.get(sno);
        return m != null && m.isSenior();
    }

    /** 컨트롤러용: zno의 좌석을 seatName 규칙(A/B/C + 숫자)으로 정렬해 반환 */
    public List<SeatCsvDto> getSeatsByZoneSorted(int zno) {
        List<SeatCsvDto> list = new ArrayList<>(seatsListByZone.getOrDefault(zno, List.of()));
        list.sort(this::compareSeatName);
        return list;
    }

    // ── CSV 로드 ───────────────────────────────────────────────────
    private void loadZonesCsv(String path) {
        try (var reader = open(path)) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] t = line.split(",", -1);
                int zno = Integer.parseInt(t[0].trim());
                String zname = t[1].trim();
                zoneNameByZno.put(zno, zname);
            }
        } catch (Exception e) {
            System.out.println("[SeatCsvService] zones.csv load warn: " + e.getMessage());
        }
    }

    private void loadSeatsCsv(String path) {
        try (var reader = open(path)) {
            String line = reader.readLine(); // header: sno,zno,seatName,senior
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] t = line.split(",", -1);
                int sno = Integer.parseInt(t[0].trim());
                int zno = Integer.parseInt(t[1].trim());
                String seatName = t[2].trim();
                boolean senior = Boolean.parseBoolean(t[3].trim());

                var meta = new SeatCsvDto(sno, zno, seatName, senior);

                allSeatSnos.add(sno);
                zoneToSnos.computeIfAbsent(zno, k -> new HashSet<>()).add(sno);
                metaBySno.put(sno, meta);
                seatsListByZone.computeIfAbsent(zno, k -> new ArrayList<>()).add(meta);
            }
        } catch (Exception e) {
            throw new IllegalStateException("seats.csv load error: " + e.getMessage(), e);
        }
    }

    private BufferedReader open(String path) throws Exception {
        var res = new ClassPathResource(path);
        return new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8));
    }

    // ── 정렬 도우미 ────────────────────────────────────────────────
    private int compareSeatName(SeatCsvDto a, SeatCsvDto b) {
        char ra = rowChar(a.seatName);
        char rb = rowChar(b.seatName);
        if (ra != rb) return Character.compare(ra, rb);
        int na = colNum(a.seatName);
        int nb = colNum(b.seatName);
        return Integer.compare(na, nb);
    }

    private char rowChar(String s) {
        return (s != null && !s.isEmpty()) ? Character.toUpperCase(s.charAt(0)) : 'Z';
    }

    private int colNum(String s) {
        if (s == null || s.length() < 2) return Integer.MAX_VALUE;
        try { return Integer.parseInt(s.substring(1)); } catch (NumberFormatException e) { return Integer.MAX_VALUE; }
    }
}
