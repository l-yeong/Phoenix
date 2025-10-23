package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import phoenix.model.dto.TicketsDto;
import phoenix.model.mapper.TicketsMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketsService {
    private final TicketsMapper ticketsMapper;
    private final FileService fileService;

    /**
     * 예약 rno가 'reserved' 상태일 때만 QR 문자열(이미지 경로) 생성 후 tickets 테이블에 저장합니다.
     * rno로 예약/좌석/회원 정보 조회
     * 예약 상태가 'reserved'인지 확인 (아니면 종료)
     * 해당 rno에 이미 ticket_code가 존재하는지 확인 (중복 발급 방지)
     * QR payload 구성(이름/구역/좌석/사용여부) → QR 이미지 생성/저장 → 이미지 경로 반환
     * tickets에 (rno, ticket_code, price, valid=true) 저장
     * <p>
     * * - @Transactional: 읽기/쓰기 포함. DB 갱신 단위 보장.
     *
     * @param rno 예매 고유번호
     * @return true: 신규 발급 성공 / false: 상태 부적합 또는 중복 존재 등으로 미발급
     * <p>
     * <연계 컨트롤러 예시>
     * - POST /tickets/write?rno={rno}
     */
    @Transactional
    public boolean ticketWrite(int rno) {
        //예약 정보 조회 + 상태 검증
        Map<String, Object> info = ticketsMapper.ticketPrint(rno);
        if (info == null) return false;

        String status = String.valueOf(info.get("reservation_status"));
        if (!"reserved".equalsIgnoreCase(status)) {
            System.out.println("[ticketWrite] 상태가 reserved 아님 ->false");
            return false;
        }//func end

        // rno 중복 QR발급 방지
        String existingCode = ticketsMapper.findTicketdedupe(rno);
        if (existingCode != null && !existingCode.isEmpty()) {
            System.out.println("[ticketWrite] 이미 QR 존재 → false");
            return false;
        }//if end

        // 6자리 토큰 생성 (예: ab12f9)
        String qrUuid = java.util.UUID.randomUUID()
                .toString().replace("-", "")
                .substring(0, 6);

        // QR코드 스캔 URL(도메인생기면 여기만 수정)
        String baseUrl = "http://localhost:8080";
        String qrUrl = baseUrl + "/tickets/qr?qr=" + qrUuid;

        // QR 이미지 파일 생성 및 저장
        String imagePath = fileService.saveQRImg(qrUrl);

        //DB저장
        TicketsDto dto = new TicketsDto();
        dto.setRno(rno);
        dto.setTicket_code(imagePath);
        dto.setValid(true);
        dto.setTicket_uuid(qrUuid);
        ticketsMapper.ticketWrite(dto);
        return true;

    }//func end

    /**
     * 회원별 QR payload(예: QR 이미지 URL/경로) 목록을 조회합니다.
     * <p>
     * <트랜잭션>
     * - @Transactional(readOnly = true): 읽기 전용
     *
     * @param mno 회원 고유번호
     * @return 해당 회원의 티켓 payload(이미지 경로 등) 리스트
     * <p>
     * <연계 컨트롤러 예시>
     * - GET /tickets/print?mno={mno}
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findPayloads(int mno, int rno) {
        return ticketsMapper.findPayloads(mno, rno);
    }//func end

    /**
     * <스케줄>
     * 매일 9시~23시 사이 5분마다 자동 실행 (KST)
     * 반환값 없음(반드시 void), 파라미터 없음(필수)
     */
    //@Scheduled(cron = "0 */5 9-23 * * *",zone = "Asia/Seoul")
    public void formerGame() {
        try {
            int updated = formerGameCSV();
            if (updated > 0) {
                System.out.println(" 티켓 만료 처리 완료 (valid 1 -> 0) : " + updated);
            }//if end
        } catch (Exception e) {
            System.out.println("티켓 만료 처리 실패" + e);
        }//catch end
    }//func end

    /**
     * CSV의 경기 날짜+시간 기준으로 지난 경기 gno를 추출하여
     * tickets.valid=0 로 일괄 업데이트.
     *
     * @return 실제로 업데이트된 행 수
     */
    @Transactional
    public int formerGameCSV() {
        List<Integer> expired = fileService.getExpiredGames(); // game.csv 호출
        if (expired.isEmpty()) return 0; // 만료된 경기가 없으면 종료

        String gnoList = expired.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return ticketsMapper.formerGame(gnoList);
    }//func end

    /**
     * uuid → rno 조회 (없으면 0)
     */
    public Map<String, Object> ticketUrlUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        return ticketsMapper.ticketUrlUuid(uuid);
    }//func end

    /**
     * uuid → 예매 상세(표시용) 바로 조회 (없으면 null)
     */
    public Map<String, Object> ticketUuidInfo(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        return ticketsMapper.ticketUuidInfo(uuid);
    }//func end

    //===========================================스캐너
    @Transactional
    public Map<String, Object> qrScanAndUpdate(String ticketCode, Integer mno, Integer rno) {
        Map<String, Object> info = ticketsMapper.qrScan(ticketCode);
        if (info == null) {
            return Map.of("success", false, "message", "티켓을 찾을 수 없습니다.");
        }

        String status = (String) info.get("reservation_status");
        Boolean valid = (Boolean) info.get("valid");

        if (!"reserved".equalsIgnoreCase(status)) {
            return Map.of("success", false, "message", "취소된 예매입니다.");
        }
        if (valid == null || !valid) {
            return Map.of("success", false, "message", "이미 사용된 티켓입니다.");
        }

        int updated = ticketsMapper.qrScanInfoUpdate(ticketCode, mno, rno);
        if (updated == 1) {
            return Map.of("success", true, "message", "사용 완료");
        } else {
            return Map.of("success", false, "message", "정보 불일치 또는 이미 사용됨");
        }
    }

}//class end
