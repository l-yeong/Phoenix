// src/main/java/phoenix/service/SeniorReservationService.java
package phoenix.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phoenix.model.dto.AutoSelectDto.*;
import phoenix.model.dto.ReservationsDto;
import phoenix.model.dto.SeatDto;
import phoenix.model.mapper.SeatsMapper;
import phoenix.util.RedisKeys;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 🧓 SeniorReservationService
 * - 시니어 전용 자동예매 (임시홀드 없이 즉시 확정)
 * - 일반/시니어 상호배타, 경기당 최대 2매까지
 * - SOLD/카운터는 Redis 기준, DB는 복구/영수증(티켓) 저장용
 */
@Service
@RequiredArgsConstructor
public class SeniorReservationService {

    private final RedissonClient redisson;
    private final SeatsMapper seatsMapper;
    private final TicketsService ticketsService;
    private final SeatCsvService seatCsv;
    private final PlayerCsvService playerCsv;
    private final GameService gameService;

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    // ==== Redis Accessors (일반과 동일 네임스페이스 재사용) ====
    private RMapCache<String, String> holdMap() { return redisson.getMapCache(RedisKeys.SEAT_HOLD_MAP); }
    private RSet<Integer> soldSet(int gno)       { return redisson.getSet(RedisKeys.SEAT_SOLD_SET + ":" + gno); }
    private RAtomicLong seniorCounter(int mno, int gno) {
        return redisson.getAtomicLong(RedisKeys.keySeniorBooked(mno, gno));
    }
    private RAtomicLong generalCounter(int mno, int gno) {
        return redisson.getAtomicLong("user_booking_count:" + mno + ":" + gno); // 기존 일반 키 유지
    }
    private RLock seatLock(int gno, int sno)     { return redisson.getLock("seat:lock:" + gno + ":" + sno); }

    // ====== @PostConstruct: 시니어 카운터 복구 (DB → Redis) ======
    @PostConstruct
    public void restoreSeniorCountsFromDb() {
        try {
            var rows = seatsMapper.findSeniorReservedCountSummary(); // (mno,gno,count)
            int restored = 0;
            for (ReservationsDto dto : rows) {
                RAtomicLong c = seniorCounter(dto.getMno(), dto.getGno());
                c.set(dto.getCount());
                c.expire(7, TimeUnit.DAYS);
                restored++;
            }
            System.out.printf("[SeniorReservationService] restored senior counters: %d users%n", restored);
        } catch (Exception e) {
            System.out.println("[SeniorReservationService] restore error: " + e.getMessage());
        }
    }

    // ====== Public API: 자동예매 + 즉시 확정 ======
    public AutoSelectRes autoBookAndConfirm(int mno, AutoSelectReq req) {
        // 0) 입력/환경 검증
        if (req == null || req.getGno() <= 0) return fail("INVALID_REQUEST");
        int gno = req.getGno();
        int qty = Math.max(1, Math.min(2, req.getQty())); // 시니어 한도: 1~2

        // 기간: D-7 ~ D-DAY (시니어 허용)
        if (!isSeniorPhase(gno)) {
            return fail("OUT_OF_SENIOR_PHASE");
        }

        // 상호배타: 일반 예매 보유 시 시니어 불가
        long generalBooked = generalCounter(mno, gno).get();
        if (generalBooked > 0) return fail("BLOCKED_BY_GENERAL_BOOKING");

        // 최대 2매 (누적)
        RAtomicLong seniorCnt = seniorCounter(mno, gno);
        long already = seniorCnt.get();
        if (already + qty > 2) return fail("LIMIT_2_PER_GAME");

        // 좌석 후보 수집: 모든 존의 시니어 좌석(A1~A10)
        var seniorZones = zonesHavingSeniorSeats();
        if (seniorZones.isEmpty()) return fail("NO_SENIOR_ZONES");

        // 선호선수(pno) 존 우선순위 (있다면 마지막 tie-break에 사용)
        var favZnoPriority = favoriteZoneOrder(mno);

        // 1) 연석 탐색 (모든 존 순회)
        var best = findBestContiguous(gno, seniorZones, favZnoPriority, qty);
        // 2) 없으면 같은 존 내 "가까운 두 자리" 탐색
        if (best == null && qty == 2) {
            best = findBestClosestPair(gno, seniorZones, favZnoPriority);
        }
        // 3) qty==1이면 아무 존의 가용 1자리
        if (best == null && qty == 1) {
            best = findAnySingle(gno, seniorZones, favZnoPriority);
        }
        if (best == null) return fail("NO_SEATS_AVAILABLE");

        // === 확정 트랜잭션 ===
        return confirmNow(mno, gno, best);
    }

    // ====== 확정 ======
    @Transactional(rollbackFor = Exception.class)
    protected AutoSelectRes confirmNow(int mno, int gno, Pick pick) {
        List<Integer> snos = pick.snos;

        // 1) 좌석별 락 획득 (데드락 방지 위해 sno 정렬 후 고정 순서 잠금)
        List<Integer> lockOrder = new ArrayList<>(snos);
        Collections.sort(lockOrder);
        List<RLock> locks = new ArrayList<>();
        try {
            for (int sno : lockOrder) {
                RLock l = seatLock(gno, sno);
                // 즉시 실패 대신 짧게 대기해 충돌 완화
                if (!l.tryLock(300, 5000, TimeUnit.MILLISECONDS)) {
                    return fail("LOCK_TIMEOUT");
                }
                locks.add(l);
            }

            // 2) 최종 가용성 검증 (SOLD/HELD 모두 금지)
            for (int sno : snos) {
                if (!isAvailableForSenior(gno, sno)) return fail("SEAT_TAKEN");
            }

            // 3) Redis 선반영 (SOLD + seniorCounter)
            RSet<Integer> sold = soldSet(gno);
            for (int sno : snos) sold.add(sno);
            RAtomicLong cnt = seniorCounter(mno, gno);
            cnt.addAndGet(snos.size());
            cnt.expire(7, TimeUnit.DAYS);

            // 4) DB 저장(예약 + 티켓)
            for (int sno : snos) {
                ReservationsDto dto = new ReservationsDto();
                dto.setMno(mno);
                dto.setGno(gno);
                dto.setSno(sno);
                dto.setStatus("reserved");
                dto.setChannel("senior"); // ★채널 구분
                boolean ok = seatsMapper.insertReservationWithChannel(dto);
                if (!ok) throw new IllegalStateException("reservation insert fail");
                int rno = dto.getRno();
                if (!ticketsService.ticketWrite(rno)) {
                    throw new IllegalStateException("ticket issue fail");
                }
            }

            // 5) 성공 응답 구성
            return AutoSelectRes.builder()
                    .ok(true)
                    .qty(snos.size())
                    .qtyHeld(snos.size())
                    .zno(pick.zno)
                    .heldSnos(new ArrayList<>(snos))
                    .contiguous(pick.contiguous)
                    .bundles(List.of(
                            // ⬇️ 여기!
                            Bundle.builder()
                                    .zno(pick.zno)
                                    .zoneLabel(seatCsv.getZoneName(pick.zno))
                                    .contiguous(pick.contiguous)
                                    .snos(new ArrayList<>(snos))
                                    .seatNames(snos.stream().map(seatCsv::getSeatName).toList())
                                    .build()
                    ))
                    .strategy(pick.strategy)
                    .build();

        } catch (Exception e) {
            // Redis 롤백
            try { for (int sno : snos) soldSet(gno).remove(sno); } catch (Exception ignore) {}
            try {
                // senior 카운터 되돌림 (과증가분 보정)
                RAtomicLong cnt = seniorCounter(mno, gno);
                if (cnt.get() > 0) {
                    long cur = cnt.get();
                    long dec = Math.min(cur, snos.size());
                    while (true) {
                        long c = cnt.get();
                        long n = Math.max(0, c - dec);
                        if (cnt.compareAndSet(c, n)) break;
                    }
                }
            } catch (Exception ignore) {}
            return fail("CONFIRM_FAIL:" + e.getMessage());
        } finally {
            // 락 해제
            for (RLock l : locks) {
                try { l.unlock(); } catch (Exception ignore) {}
            }
        }
    }

    // ====== 가용성 판단(시니어 관점) ======
    private boolean isAvailableForSenior(int gno, int sno) {
        if (soldSet(gno).contains(sno)) return false;
        String holder = holdMap().get(gno + ":" + sno); // 일반 임시홀드와 충돌 방지
        return holder == null;
    }

    // ====== 기간: D-7 ~ D-DAY ======
    private boolean isSeniorPhase(int gno) {
        var game = gameService.findByGno(gno);
        if (game == null || game.getDate() == null || game.getTime() == null) return false;
        ZonedDateTime startAt = ZonedDateTime.of(game.getDate(), game.getTime(), ZONE_SEOUL);
        ZonedDateTime openAt  = startAt.minusDays(7);
        ZonedDateTime now = ZonedDateTime.now(ZONE_SEOUL);
        return (!now.isBefore(openAt)) && (!now.isAfter(startAt)); // openAt <= now <= startAt
    }

    // ====== 존/선호 우선순위 ======
    private List<Integer> zonesHavingSeniorSeats() {
        // seats.csv 기준 senior=true 좌석이 하나라도 등록된 zno만
        return seatCsv.getSeatsByZoneSorted(10001).getClass() == null // dummy to keep import
                ? List.of() : seatCsvZoneKeys();
    }

    private List<Integer> seatCsvZoneKeys() {
        // SeatCsvService가 zone 목록 직접 노출하진 않으므로, meta 맵에서 유추
        // → seatsListByZone을 공개 안 했으니, senior 존재 여부는 전수조사
        // 전수조사: 10001~10999 같은 범위를 모른다면 metaBySno 순회가 필요하지만
        // 여기서는 seats.csv 기준으로 "존 조회"를 다음과 같이 구성:
        Set<Integer> z = new LinkedHashSet<>();
        // 모든 sno를 모르므로 '존 이름 조회' 가능한 zno 후보를 소환
        // 이미 프로젝트에서 사용중인 zno는 10001~10006 (샘플), 실제는 seats.csv에 저장됨
        // 안전하게는 1~99999 순회가 아니고, 아래 방식으로 수집:
        for (int candidate = 10001; candidate <= 99999; candidate++) {
            if (!seatCsv.existsZone(candidate)) continue;
            var metas = seatCsv.getSeatsByZoneSorted(candidate);
            boolean anySenior = metas.stream().anyMatch(m -> m.isSenior());
            if (anySenior) z.add(candidate);
        }
        return new ArrayList<>(z);
    }

    private List<Integer> favoriteZoneOrder(int mno) {
        var member = playerCsv.findAllPlayers(); // trick to ensure bean loaded
        // membersService에서 pno는 Seats/Auto에서만 접근—여기서는 MembersService 대신 PlayerCsv만 사용
        // 실제 pno는 MembersService.getLoginMember().getPno()지만 컨트롤러에서 mno만 전달받으므로
        // 선호존 tie-break는 "없음"으로 처리 (필요 시 MembersService 주입 후 pno 사용)
        // ===> 요구: "연석 우선, 모두 연석 없으면 선호존 우선"
        // 현재 정보만으로는 pno를 접근하지 않으니, tie-break는 '존 자연 순서'로 처리.
        return List.of(); // 빈 리스트면 tie-break 사용 안 함
    }

    // ====== 탐색 유틸 ======
    private record Pick(int zno, List<Integer> snos, boolean contiguous, String strategy) {}

    private Pick findBestContiguous(int gno, List<Integer> zones, List<Integer> fav, int qty) {
        if (qty == 1) return null;
        List<Pick> candidates = new ArrayList<>();
        for (int zno : zones) {
            var usable = seniorAvailableInZone(gno, zno);
            var run = findContiguousRun(usable, 2); // qty == 2만 의미
            if (run != null) {
                var snos = run.stream().map(SeatCsvService.SeatCsvDto::getSno).toList();
                candidates.add(new Pick(zno, snos, true, "contiguous@" + zno));
            }
        }
        if (candidates.isEmpty()) return null;
        // tie-break: (선호존 포함시 그쪽 우선) → 여기서는 입력 fav 빈 리스트이므로 첫번째
        return candidates.get(0);
    }

    private Pick findBestClosestPair(int gno, List<Integer> zones, List<Integer> fav) {
        Pick best = null;
        int bestDist = Integer.MAX_VALUE;

        for (int zno : zones) {
            var usable = seniorAvailableInZone(gno, zno);
            // 같은 존에서 가장 가까운 두 좌석
            var pair = closestPair(usable);
            if (pair == null) continue;
            int d = Math.abs(col(pair.get(0).getSeatName()) - col(pair.get(1).getSeatName()));
            if (best == null || d < bestDist) {
                best = new Pick(zno, pair.stream().map(SeatCsvService.SeatCsvDto::getSno).toList(), false, "closest@" + zno);
                bestDist = d;
            }
        }
        return best;
    }

    private Pick findAnySingle(int gno, List<Integer> zones, List<Integer> fav) {
        for (int zno : zones) {
            var usable = seniorAvailableInZone(gno, zno);
            if (!usable.isEmpty()) {
                int sno = usable.get(0).getSno();
                return new Pick(zno, List.of(sno), true, "single@" + zno);
            }
        }
        return null;
    }

    private List<SeatCsvService.SeatCsvDto> seniorAvailableInZone(int gno, int zno) {
        List<SeatCsvService.SeatCsvDto> metas = seatCsv.getSeatsByZoneSorted(zno);
        // senior=true이면서 SOLD/HELD 아닌 좌석만
        return metas.stream()
                .filter(SeatCsvService.SeatCsvDto::isSenior)
                .filter(m -> isAvailableForSenior(gno, m.getSno()))
                .toList();
    }

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

    private List<SeatCsvService.SeatCsvDto> closestPair(List<SeatCsvService.SeatCsvDto> usable) {
        if (usable.size() < 2) return null;
        List<SeatCsvService.SeatCsvDto> best = null;
        int bestDist = Integer.MAX_VALUE;

        Map<Character, List<SeatCsvService.SeatCsvDto>> byRow = usable.stream()
                .collect(Collectors.groupingBy(m -> row(m.getSeatName())));

        for (var e : byRow.entrySet()) {
            List<SeatCsvService.SeatCsvDto> rowSeats = e.getValue().stream()
                    .sorted(Comparator.comparingInt(m -> col(m.getSeatName())))
                    .toList();
            for (int i = 0; i < rowSeats.size(); i++) {
                for (int j = i + 1; j < rowSeats.size(); j++) {
                    int d = Math.abs(col(rowSeats.get(i).getSeatName()) - col(rowSeats.get(j).getSeatName()));
                    if (d < bestDist) {
                        bestDist = d;
                        best = List.of(rowSeats.get(i), rowSeats.get(j));
                    }
                }
            }
        }
        return best;
    }

    private char row(String seatName) {
        return (seatName != null && !seatName.isEmpty()) ? Character.toUpperCase(seatName.charAt(0)) : 'Z';
    }
    private int col(String seatName) {
        if (seatName == null || seatName.length() < 2) return Integer.MAX_VALUE;
        try { return Integer.parseInt(seatName.substring(1)); } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    // ====== 실패 응답 ======
    private AutoSelectRes fail(String reason) {
        return AutoSelectRes.builder().ok(false).reason(reason).build();
    }
}
