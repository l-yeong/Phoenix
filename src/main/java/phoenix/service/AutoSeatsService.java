package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import phoenix.model.dto.AutoSelectDto.*;
import phoenix.model.dto.GameDto;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 자동예매 핵심 로직
 *  - 홈/어웨이 제약 + 중립 허용(팬사이드 기준)
 *  - 선호선수 포지션 가중(경기 팀이면만 적용)
 *  - 시니어석: 일반예매에서는 경기 시작 D-2 전까지 제외
 *  - 우선순위: "단일 존에서 연석 → 단일 존에서 비연석 → (최후수단) 멀티존 폴백(부분확보 포함)"
 */
@Service
@RequiredArgsConstructor
public class AutoSeatsService {

    private static final int HOLD_TTL_SECONDS = 120;

    private final SeatCsvService seatCsv;       // seats.csv (sno,zno,seatName,senior)
    private final SeatLockService seatLocks;    // tryLockSeat, getSeatStatusFor 등
    private final GameService gameService;      // games.csv
    private final MembersService membersService;
    private final PlayerCsvService playerCsv;   // (간단 CSV) pno→team, position

    // ★ 프로젝트별 실제 zno에 맞춰 조정
    public enum ZoneKind { HOME_INFIELD, HOME_3B, AWAY_1B, NEUTRAL_CATCHER, NEUTRAL_OUTFIELD }

    private static final Map<Integer, ZoneKind> ZONE_KIND = createZoneKindMap();
    private static Map<Integer, ZoneKind> createZoneKindMap() {
        Map<Integer, ZoneKind> m = new LinkedHashMap<>();
        m.put(10001, ZoneKind.HOME_INFIELD);     // 연우석
        m.put(10002, ZoneKind.AWAY_1B);          // 겨레석
        m.put(10003, ZoneKind.HOME_3B);          // 찬영석
        m.put(10004, ZoneKind.HOME_3B);          // 성호석
        m.put(10005, ZoneKind.NEUTRAL_CATCHER);  // 중앙테이블석(중립)
        m.put(10006, ZoneKind.NEUTRAL_OUTFIELD); // 외야자유석(중립)
        return m;
    }

    // ───────────────────────────────────────────────────────────
    // Public API
    // ───────────────────────────────────────────────────────────
    public AutoSelectRes autoAssignAndHold(int mno, AutoSelectReq req) {
        // 0) 입력 검증 & 환경 조회
        if (req.getQty() < 1 || req.getQty() > 4) return fail("QTY_OUT_OF_RANGE(1~4)");
        GameDto game = gameService.findByGno(req.getGno());
        if (game == null) return fail("GAME_NOT_FOUND");

        // 경기 시작 시각(D-2 게이팅)
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime gameAt = ZonedDateTime.of(game.getDate(), game.getTime(), zone);
        boolean seniorGateOn = isBeforeGeneralSeniorOpen(gameAt); // D-2 전이면 true

        // 회원 선호선수 → 팀/포지션
        var member = membersService.getLoginMember();
        Integer pno = member != null ? member.getPno() : null;
        PlayerCsvService.PlayerInfo fav = (pno != null) ? playerCsv.findByPno(pno) : null;

        String favTeamName = (fav != null) ? playerCsv.findTeamName(fav.getTeamNo()) : null;
        boolean favInThisGame =
                favTeamName != null &&
                        (favTeamName.equals(game.getHomeTeam()) || favTeamName.equals(game.getAwayTeam()));

        // 팬사이드 정규화
        String side = (req.getFanSide() == null) ? "ANY" : req.getFanSide().trim().toUpperCase(Locale.ROOT);
        if (!side.equals("HOME") && !side.equals("AWAY")) side = "ANY";

        boolean allowCrossZone = req.getCrossZone() == null ? true : req.getCrossZone();

        // 1) 존 우선순위 생성(홈/어웨이/중립 제약 + 포지션 가중)
        List<Integer> zonePriority = buildZonePriority(game, side, favInThisGame ? fav.getPosition() : null);

        List<String> hopTrace = new ArrayList<>();

        // 2) 단일 존 루프 — 연석 → 비연석
        for (int zno : zonePriority) {
            if (!seatCsv.existsZone(zno)) continue;

            // 메타/상태 조회
            List<SeatCsvService.SeatCsvDto> metas = seatCsv.getSeatsByZoneSorted(zno);
            List<Integer> snos = metas.stream().map(SeatCsvService.SeatCsvDto::getSno).toList();
            Map<Integer, String> status = seatLocks.getSeatStatusFor(req.getGno(), mno, snos);

            // 일반예매에서 usable만 필터 (시니어석 D-2 전 제외)
            List<SeatCsvService.SeatCsvDto> usable = metas.stream()
                    .filter(m -> isUsableForGeneral(status.get(m.getSno()), m.isSenior(), seniorGateOn))
                    .toList();

            // 2-1) 연석
            if (req.isPreferContiguous()) {
                List<SeatCsvService.SeatCsvDto> run = findContiguousRun(usable, req.getQty());
                if (run != null) {
                    HoldResult hold = tryHoldAll(mno, req.getGno(), zno, run);
                    if (hold.ok) {
                        hopTrace.add("contiguous@" + zno);
                        return oneZoneSuccess(req, zno, hold.held, true, hopTrace);
                    } else hopTrace.add("contiguous-failed@" + zno + ":" + hold.reason);
                } else hopTrace.add("no-run@" + zno);
            }

            // 2-2) 단일 존 비연석
            List<SeatCsvService.SeatCsvDto> singles = pickSingles(usable, req.getQty());
            if (!singles.isEmpty()) {
                HoldResult hold = tryHoldAll(mno, req.getGno(), zno, singles);
                if (hold.ok) {
                    hopTrace.add("singles@" + zno);
                    return oneZoneSuccess(req, zno, hold.held, false, hopTrace);
                } else hopTrace.add("singles-failed@" + zno + ":" + hold.reason);
            }
        }

        // 3) (최후수단) 멀티존 폴백 — 요청 수량을 여러 존에서 합쳐서 확보(부분확보 허용)
        //    - 팬사이드 제약 내에서 zonePriority 순서대로 그리디하게 확보
        List<Bundle> bundles = new ArrayList<>();
        int remain = req.getQty();

        for (int zno : zonePriority) {
            if (remain <= 0) break;
            if (!seatCsv.existsZone(zno)) continue;

            // 메타/상태
            List<SeatCsvService.SeatCsvDto> metas = seatCsv.getSeatsByZoneSorted(zno);
            List<Integer> snos = metas.stream().map(SeatCsvService.SeatCsvDto::getSno).toList();
            Map<Integer, String> status = seatLocks.getSeatStatusFor(req.getGno(), mno, snos);

            List<SeatCsvService.SeatCsvDto> usable = metas.stream()
                    .filter(m -> isUsableForGeneral(status.get(m.getSno()), m.isSenior(), seniorGateOn))
                    .toList();

            // 우선: 이 존에서 최대 연석(남은 수량 이하) 일부라도 잡아보기
            List<SeatCsvService.SeatCsvDto> bestRun = findBestRunUpTo(usable, remain);
            HoldResult gotRun = tryHoldSome(mno, req.getGno(), zno, bestRun, remain);

            int heldHere = gotRun.held.size();
            remain -= heldHere;

            // 부족하면 싱글로 메우기
            if (remain > 0) {
                List<SeatCsvService.SeatCsvDto> pool = usable.stream()
                        .filter(s -> !gotRun.held.contains(s.getSno()))
                        .toList();
                HoldResult gotSingles = tryHoldSome(mno, req.getGno(), zno, pool, remain);
                gotRun.held.addAll(gotSingles.held);
                remain -= gotSingles.held.size();
            }

            // 이 존에서 뭔가라도 확보했으면 번들 추가
            if (!gotRun.held.isEmpty()) {
                bundles.add(toBundle(zno, gotRun.held));
                hopTrace.add("multi@" + zno + "=" + gotRun.held.size());
            }
        }

        int heldTotal = bundles.stream().mapToInt(b -> b.getSnos().size()).sum();
        if (heldTotal > 0) {
            // 부분확보라도 ok=true로 반환 (요청사항: 남은 좌석이라도 임시 홀드해주기)
            return AutoSelectRes.builder()
                    .ok(true)
                    .reason(heldTotal < req.getQty() ? "PARTIAL" : null)
                    .strategy(String.join(">", hopTrace))
                    .ttlSec(HOLD_TTL_SECONDS)
                    .qty(req.getQty())
                    .qtyHeld(heldTotal)
                    .bundles(bundles)
                    .build();
        }

        // 4) 전혀 못 잡음
        return AutoSelectRes.builder()
                .ok(false)
                .reason("NO_SEATS_MATCH_RULES")
                .strategy(String.join(">", hopTrace))
                .build();
    }

    // ── Senior gating: 일반예매 D-2 전이면 시니어석 제외 ──
    private boolean isBeforeGeneralSeniorOpen(ZonedDateTime gameAt) {
        return ZonedDateTime.now(gameAt.getZone()).isBefore(gameAt.minusHours(48));
    }

    private boolean isUsableForGeneral(String status, boolean senior, boolean seniorGateOn) {
        if (status == null) return false;
        if ("SOLD".equals(status) || "HELD".equals(status) || "HELD_BY_ME".equals(status)) return false;
        if ("INVALID".equals(status)) return false;
        if (senior && seniorGateOn) return false;   // 핵심: D-2 전 시니어석 제외
        if ("BLOCKED".equals(status)) return false; // 서버가 BLOCKED로 주는 경우
        return "AVAILABLE".equals(status);
    }

    // ── 존 우선순위 (팬사이드 + 포지션 힌트) ──
    private List<Integer> buildZonePriority(GameDto game, String side, String position) {
        // 홈/어웨이/중립 그룹
        List<ZoneKind> home    = Arrays.asList(ZoneKind.HOME_3B, ZoneKind.HOME_INFIELD);
        List<ZoneKind> away    = Arrays.asList(ZoneKind.AWAY_1B);
        List<ZoneKind> neutral = Arrays.asList(ZoneKind.NEUTRAL_CATCHER, ZoneKind.NEUTRAL_OUTFIELD);

        List<ZoneKind> base;
        if ("HOME".equals(side))      base = concat(home, neutral);     // HOME + NEUTRAL 허용
        else if ("AWAY".equals(side)) base = concat(away, neutral);     // AWAY + NEUTRAL 허용
        else                          base = concat(concat(home, away), neutral); // ANY: 모두

        // 포지션 가중(선호선수가 이 경기 팀일 때만)
        if (position != null) {
            ZoneKind hint = positionHint(position);
            base = reprioritize(base, hint);
        }

        // kind → zno
        List<Integer> result = new ArrayList<>();
        for (ZoneKind k : base) {
            for (Map.Entry<Integer, ZoneKind> e : ZONE_KIND.entrySet()) {
                if (e.getValue() == k) result.add(e.getKey());
            }
        }
        return result.stream().distinct().filter(seatCsv::existsZone).collect(Collectors.toList());
    }

    // 포지션 매핑(요청 확장: 좌/중/우익수, 2루/유격수 등)
    private ZoneKind positionHint(String posRaw) {
        String p = posRaw.trim();
        if (p.contains("포수")) return ZoneKind.NEUTRAL_CATCHER;

        // 1루/3루
        if (p.contains("1루")) return ZoneKind.AWAY_1B;
        if (p.contains("3루")) return ZoneKind.HOME_3B;

        // 외야(좌/중/우)
        if (p.contains("좌익수") || p.contains("중견수") || p.contains("우익수") || p.contains("외야"))
            return ZoneKind.NEUTRAL_OUTFIELD;

        // 내야(2루/유격수 등)
        if (p.contains("2루") || p.contains("유격")) return ZoneKind.HOME_INFIELD;

        // 투수/그 외 → 내야
        if (p.contains("투수")) return ZoneKind.HOME_INFIELD;

        return ZoneKind.HOME_INFIELD;
    }

    private static <T> List<T> concat(List<T> a, List<T> b) { List<T> r = new ArrayList<>(a); r.addAll(b); return r; }

    private static <T> List<T> reprioritize(List<T> list, T firstIfFound) {
        if (firstIfFound == null || !list.contains(firstIfFound)) return list;
        List<T> r = new ArrayList<>();
        r.add(firstIfFound);
        for (T t : list) if (!Objects.equals(t, firstIfFound)) r.add(t);
        return r;
    }

    // ── 같은 행에서 연속 번호 qty개 찾기 (A1, A2, …) ──
    private List<SeatCsvService.SeatCsvDto> findContiguousRun(List<SeatCsvService.SeatCsvDto> usable, int qty) {
        Map<Character, List<SeatCsvService.SeatCsvDto>> byRow = usable.stream()
                .collect(Collectors.groupingBy(m -> row(m.getSeatName())));
        for (var entry : byRow.entrySet()) {
            List<SeatCsvService.SeatCsvDto> rowSeats = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(m -> col(m.getSeatName())))
                    .toList();
            for (int i = 0; i + qty - 1 < rowSeats.size(); i++) {
                boolean ok = true;
                int start = col(rowSeats.get(i).getSeatName());
                for (int k = 1; k < qty; k++) {
                    if (col(rowSeats.get(i + k).getSeatName()) != start + k) { ok = false; break; }
                }
                if (ok) return rowSeats.subList(i, i + qty);
            }
        }
        return null;
    }

    // 멀티존 수집용: 남은 수량 이하에서 가장 긴 연석을 뽑아 반환(없으면 빈 리스트)
    private List<SeatCsvService.SeatCsvDto> findBestRunUpTo(List<SeatCsvService.SeatCsvDto> usable, int limit) {
        Map<Character, List<SeatCsvService.SeatCsvDto>> byRow = usable.stream()
                .collect(Collectors.groupingBy(m -> row(m.getSeatName())));
        List<SeatCsvService.SeatCsvDto> best = List.of();
        for (var entry : byRow.entrySet()) {
            List<SeatCsvService.SeatCsvDto> rowSeats = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(m -> col(m.getSeatName())))
                    .toList();

            // 슬라이딩으로 run 길이 측정(최대 limit)
            int n = rowSeats.size();
            int i = 0;
            while (i < n) {
                int j = i;
                int start = col(rowSeats.get(i).getSeatName());
                while (j + 1 < n && col(rowSeats.get(j + 1).getSeatName()) == col(rowSeats.get(j).getSeatName()) + 1) {
                    j++;
                }
                int len = Math.min(j - i + 1, limit);
                if (len > best.size()) best = rowSeats.subList(i, i + len);
                i = j + 1;
            }
        }
        return best;
    }

    private List<SeatCsvService.SeatCsvDto> pickSingles(List<SeatCsvService.SeatCsvDto> usable, int qty) {
        if (usable.size() < qty) return Collections.emptyList();
        return new ArrayList<>(usable.subList(0, qty));
    }

    private char row(String seatName) {
        return (seatName != null && !seatName.isEmpty()) ? Character.toUpperCase(seatName.charAt(0)) : 'Z';
    }
    private int col(String seatName) {
        if (seatName == null || seatName.length() < 2) return Integer.MAX_VALUE;
        try { return Integer.parseInt(seatName.substring(1)); } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    // ── 좌석 홀드 유틸 ────────────────────────────────────────────
    private static class HoldResult { boolean ok; String reason; List<Integer> held = new ArrayList<>(); }

    /** 단일 시도에서 모두 성공해야 OK, 하나라도 실패하면 롤백 */
    private HoldResult tryHoldAll(int mno, int gno, int zno, List<SeatCsvService.SeatCsvDto> seats) {
        HoldResult r = new HoldResult();
        for (SeatCsvService.SeatCsvDto m : seats) {
            int code;
            try { code = seatLocks.tryLockSeat(mno, gno, zno, m.getSno()); }
            catch (InterruptedException e) { code = -99; }
            if (code == 1) { r.held.add(m.getSno()); continue; }
            r.ok = false; r.reason = "lock-fail:" + code;
            rollbackHolds(mno, gno, zno, r.held); r.held.clear();
            return r;
        }
        r.ok = true; r.reason = "OK"; return r;
    }

    /** 일부만 성공해도 유지(멀티존 그리디 수집용) — 실패 좌석은 건너뛴다 */
    private HoldResult tryHoldSome(int mno, int gno, int zno, List<SeatCsvService.SeatCsvDto> seats, int limit) {
        HoldResult r = new HoldResult(); r.ok = false; r.reason = "none";
        if (seats == null || seats.isEmpty() || limit <= 0) return r;
        for (SeatCsvService.SeatCsvDto m : seats) {
            if (r.held.size() >= limit) break;
            int code;
            try { code = seatLocks.tryLockSeat(mno, gno, zno, m.getSno()); }
            catch (InterruptedException e) { code = -99; }
            if (code == 1) {
                r.held.add(m.getSno());
                r.ok = true; r.reason = "OK_SOME";
            } else {
                r.reason = "lock-fail:" + code; // 기록만, 계속 진행
            }
        }
        return r;
    }

    private void rollbackHolds(int mno, int gno, int zno, List<Integer> snos) {
        for (int sno : snos) {
            try { seatLocks.releaseSeat(mno, gno, zno, sno); } catch (Exception ignore) {}
        }
    }

    private Bundle toBundle(int zno, List<Integer> snos) {
        List<String> names = snos.stream().map(seatCsv::getSeatName).toList();
        return Bundle.builder()
                .zno(zno)
                .zoneLabel(seatCsv.getZoneName(zno))
                .contiguous(false)              // 멀티존 수집은 기본 false(칩에서만 의미)
                .snos(new ArrayList<>(snos))
                .seatNames(new ArrayList<>(names))
                .build();
    }

    private AutoSelectRes oneZoneSuccess(AutoSelectReq req, int zno, List<Integer> held, boolean contiguous, List<String> trace) {
        // 단일존 성공은 bundles 1개로도 내려주되 레거시 필드도 세팅
        Bundle b = Bundle.builder()
                .zno(zno)
                .zoneLabel(seatCsv.getZoneName(zno))
                .contiguous(contiguous)
                .snos(held)
                .seatNames(held.stream().map(seatCsv::getSeatName).toList())
                .build();

        return AutoSelectRes.builder()
                .ok(true)
                .strategy(String.join(">", trace))
                .ttlSec(HOLD_TTL_SECONDS)
                .qty(req.getQty())
                .qtyHeld(held.size())
                .bundles(List.of(b))
                .zno(zno)
                .heldSnos(held)
                .contiguous(contiguous)
                .build();
    }

    // ── 실패 응답 헬퍼 ────────────────────────────────────────────
    private AutoSelectRes fail(String reason) {
        return AutoSelectRes.builder().ok(false).reason(reason).build();
    }
}
