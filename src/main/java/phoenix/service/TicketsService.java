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
     * 예약(rno)이 'reserved' 상태일 때만 QR 코드를 생성하여 티켓을 발급.
     * <p>
     * 절차:
     * 1. 예약/좌석/회원 정보 조회
     * 2. 예약 상태가 'reserved'인지 검증
     * 3. 기존 발급 티켓 여부 중복 확인
     * 4. QR UUID 생성 → 스캔 URL 구성
     * 5. QR 이미지 생성 및 저장 (FileService 활용)
     * 6. DB에 신규 티켓 정보 저장
     * @Transactional: 읽기/쓰기 포함 트랜잭션 (DB 일관성 보장)
     *
     * @param rno 예매 고유번호
     * @return true - 발급 성공 / false - 상태 부적합 또는 중복 존재 등으로 실패
     *
     * <연계 컨트롤러 예시>
     * POST /tickets/write?rno={rno}
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
        String baseUrl = "http://192.168.40.190:5173";
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

    //티켓취소
    public boolean ticketCancel(int rno){
        boolean result = ticketsMapper.ticketCancel(rno);
        return result;
    }//func end

    /**
     * QR 이미지 정리 스케줄러
     * - 유효(valid=0) 상태인 티켓의 QR 이미지 파일만 물리적으로 삭제.
     * - DB의 ticket_code 컬럼 값은 통계/로그 분석을 위해 유지.
     * - 하루 최대 1,000개의 파일을 삭제하며, 예외 발생 시 개별 로그 출력.
     */
//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void QRImgDelete() {
        final int DeleteCount = 1000; // 하루에 최대 1000개까지 QR이미지 삭제
        while (true) {
            List<String> imgDelete = ticketsMapper.QRImgDelete(DeleteCount);
            if (imgDelete == null || imgDelete.isEmpty()) break;

            int deleted = 0;
            for (String qrDelete : imgDelete) {
                if (qrDelete == null) continue;
                try {
                    //QR이미지 파일만 삭제
                    if (fileService.deleteQRImg(qrDelete)) deleted++;
                } catch (Exception e) {
                    System.out.println("[QR 이미지 삭제 실패] " + qrDelete + " | " + e.getMessage());
                }//catch end
            }//for end
            if (deleted > 0) {
                System.out.println("[QR 이미지 삭제 수] " + deleted);
            }//if end
            break;
        }//while end
    }//func end

    /**
     * 회원별 티켓 QR payload 목록 조회

     * - 발급된 티켓의 이미지 경로, 유효상태, 가격 등의 정보를 반환
     *
     * @Transactional(readOnly = true): 읽기 전용 트랜잭션
     *
     * @param mno 회원 고유번호
     * @param rno 예매 고유번호
     * @return 티켓 payload 리스트 (이미지 경로, 발급일 등 포함)
     *
     * <연계 컨트롤러 예시>
     * GET /tickets/print?mno={mno}&rno={rno}
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findPayloads(int mno, int rno) {
        return ticketsMapper.findPayloads(mno, rno);
    }//func end

    /**
     * 지난 경기 티켓을 자동으로 무효화(valid=0) 처리하는 스케줄러
     * - 매일 9시~23시 사이, 5분 주기로 실행되도록 설정 가능
     * - 내부적으로 formerGameCSV()를 호출하여 처리
     */

//    @Scheduled(cron = "0 */5 9-23 * * *", zone = "Asia/Seoul")
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
     * CSV의 경기 일정 파일을 기반으로 지난 경기(gno) 목록을 추출
     * 해당 경기의 티켓 valid 값을 0으로 일괄 업데이트
     *
     * @Transactional: 일괄 업데이트 트랜잭션
     *
     * @return 실제로 업데이트된 행 수
     */
    @Transactional
    public int formerGameCSV() {
        List<Integer> expired = fileService.getExpiredGames(); // game.csv 호출
        if (expired.isEmpty()) return 0; // 만료된 경기가 없으면 종료
        return ticketsMapper.formerGame(expired);
    }//func end

    /**
     * UUID를 이용해 예매 상세 정보를 즉시 조회
     *
     * @param uuid 티켓 UUID
     * @return 예매 상세 정보 맵 (없을 경우 null)
     */
    public Map<String, Object> ticketUuidInfo(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        return ticketsMapper.ticketUuidInfo(uuid);
    }//func end

    /**
     * QR 스캐너에서 티켓을 스캔했을 때 실행되는 로직
     * <p>
     * 절차:
     * 1. UUID로 티켓 유효상태(valid) 조회
     * 2. 이미 사용된 티켓(valid=false)이면 실패 응답
     * 3. valid=1 → 0 업데이트 후 “사용 완료” 메시지 반환
     *
     * @param uuid 티켓 UUID
     * @return QR 스캔 처리 결과(success/message)
     */
    @Transactional
    public Map<String, Object> qrScan(String uuid) {
        Map<String, Object> ticket = ticketsMapper.qrScan(uuid);
        if (ticket == null) {
            return Map.of("success", false, "message", "유효하지 않은 QR 코드입니다.");
        }

        boolean valid = (Boolean) ticket.get("valid");
        if (!valid) {
            return Map.of("success", false, "message", "이미 사용된 티켓입니다.");
        }

        int updated = ticketsMapper.qrScanInfoUpdate(uuid);
        if (updated == 1) {
            return Map.of("success", true, "message", "티켓 사용 완료");
        } else {
            return Map.of("success", false, "message", "이미 사용된 티켓입니다.");
        }
    }//func end

    /**
     * 관리자 페이지용 QR 사용 로그 전체 조회
     * - 티켓별 회원 정보(이름, 연락처), 좌석, 구역, 사용 여부 등을 포함
     *
     * @return 전체 티켓 로그 리스트
     */
    public List<Map<String, Object>> adminScanLog() {
        List<Map<String, Object>> result = ticketsMapper.adminScanLog();
        return result;
    }//func end

}//class end
