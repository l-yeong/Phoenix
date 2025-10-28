// phoenix/service/PlayerCsvService.java
package phoenix.service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CSV 로더
 * - team.csv  : teamno,name,logo
 * - player.csv: pno,name,pos,teamNo
 *
 * 사용 필드
 * - 팀: teamno, name
 * - 선수: pno, name, pos, teamNo
 */
@Service
public class PlayerCsvService {

    @Getter
    @AllArgsConstructor
    public static class PlayerInfo {
        private final int pno;
        private final String name;
        private final String position; // ex) "포수", "1루수", "좌익수" ...
        private final int teamNo;
    }

    private final Map<Integer, PlayerInfo> playerByPno = new HashMap<>();
    private final Map<Integer, String> teamNameByNo = new HashMap<>();

    @PostConstruct
    public void load() {
        loadTeams("static/team.csv");
        loadPlayers("static/player.csv");
        System.out.println("[PlayerCsv] loaded: teams=" + teamNameByNo.size() + ", players=" + playerByPno.size());
    }

    // ───────────────────────────────────────────────────────────
    // Public getters
    // ───────────────────────────────────────────────────────────
    public PlayerInfo findByPno(int pno) { return playerByPno.get(pno); }
    public String findTeamName(int teamNo) { return teamNameByNo.get(teamNo); }

    /** 전체 선수 목록(정렬 포함) */
    public List<PlayerInfo> findAllPlayers() {
        return playerByPno.values().stream()
                .sorted(Comparator.comparing(PlayerInfo::getTeamNo)
                        .thenComparing(PlayerInfo::getName))
                .toList(); // Java 16+: 불변 리스트
    }

    // ───────────────────────────────────────────────────────────
    // CSV loaders
    // ───────────────────────────────────────────────────────────
    private void loadTeams(String path) {
        try (var br = open(path)) {
            String line = readHeader(br); // skip header
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] t = line.split(",", -1);
                if (t.length < 2) continue; // teamno, name[, logo]

                String teamNoStr = t[0].trim();
                String name = t[1].trim();
                if (teamNoStr.isEmpty() || name.isEmpty()) continue;

                int teamno = Integer.parseInt(teamNoStr);
                teamNameByNo.put(teamno, name);
            }
        } catch (Exception e) {
            System.out.println("[PlayerCsv] teams load warn: " + e.getMessage());
        }
    }

    private void loadPlayers(String path) {
        try (var br = open(path)) {
            String line = readHeader(br); // skip header
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] t = line.split(",", -1);
                if (t.length < 4) continue; // pno,name,pos,teamNo

                String pnoStr   = t[0].trim();
                String name     = t[1].trim();
                String pos      = t[2].trim();
                String teamNoStr= t[3].trim();

                if (pnoStr.isEmpty() || name.isEmpty() || pos.isEmpty() || teamNoStr.isEmpty()) continue;

                int pno    = Integer.parseInt(pnoStr);
                int teamNo = Integer.parseInt(teamNoStr);

                playerByPno.put(pno, new PlayerInfo(pno, name, pos, teamNo));
            }
        } catch (Exception e) {
            System.out.println("[PlayerCsv] players load warn: " + e.getMessage());
        }
    }

    // ───────────────────────────────────────────────────────────
    // IO helpers
    // ───────────────────────────────────────────────────────────
    private BufferedReader open(String path) throws Exception {
        var res = new ClassPathResource(path);
        return new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8));
    }

    /** 첫 줄 헤더를 읽되, BOM이 있으면 제거해서 반환 */
    private String readHeader(BufferedReader br) throws Exception {
        String header = br.readLine();
        if (header == null) return null;
        // UTF-8 BOM 제거
        if (!header.isEmpty() && header.charAt(0) == '\uFEFF') {
            header = header.substring(1);
        }
        return header;
    }
}
