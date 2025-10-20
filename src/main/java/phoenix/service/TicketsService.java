package phoenix.service;

import lombok.RequiredArgsConstructor;
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
     *
     * * - @Transactional: 읽기/쓰기 포함. DB 갱신 단위 보장.
     *
     * @param rno 예매 고유번호
     * @return true: 신규 발급 성공 / false: 상태 부적합 또는 중복 존재 등으로 미발급
     *
     * <연계 컨트롤러 예시>
     * - POST /tickets/write?rno={rno}
     */
    @Transactional
    public boolean ticketWrite(int rno) {
        //예약 정보 조회
        Map<String, Object> info = ticketsMapper.ticketPrint(rno);
        System.out.println("rno = " + rno);
        if (info == null) return false;

        String status = String.valueOf(info.get("reservation_status"));
        if (!"reserved".equalsIgnoreCase(status)){
            System.out.println("[ticketWrite] 상태가 reserved 아님 ->false");
            return false;
        }//func end

        // 기존 QR 존재 여부 확인
        String existingCode = ticketsMapper.findTicketdedupe(rno);
        if (existingCode != null && !existingCode.isEmpty()) {
            System.out.println("[ticketWrite] 이미 QR 존재 → false");
            return false;
        }//if end

        // 가격 조회
        Number seatPrice = (Number) info.get("seat_price");
        int price = seatPrice != null ? seatPrice.intValue() : 0;

        // QR 코드용 고정 문자열
        //java.time.ZoneId KST = java.time.ZoneId.of("Asia/Seoul");
        //String date = java.time.LocalDate.now(KST)
        //        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        //String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        //String payload = String.format("%s_%s", date, uuid);

        // QR 스캔시 정보 출력
        String name = String.valueOf(info.getOrDefault("mname",""));
        String zone = String.valueOf(info.getOrDefault("zname",""));
        String seat = String.valueOf(info.getOrDefault("seat_no",""));
        String validText = "사용가능";

        Map<String,Object>qrPayload=new LinkedHashMap<>();
        qrPayload.put("이름",name);
        qrPayload.put("구역",zone);
        qrPayload.put("좌석",seat);
        qrPayload.put("사용여부",validText);


        // QR 이미지 파일 생성 및 저장
        String imagePath = fileService.saveQRImg(qrPayload);

        //DB저장
        TicketsDto dto = new TicketsDto();
        dto.setRno(rno);
        dto.setTicket_code(imagePath);
        dto.setPrice(price);
        dto.setValid(true);

        ticketsMapper.ticketWrite(dto);
        return true;
    }//func end

    /**
     * 회원별 QR payload(예: QR 이미지 URL/경로) 목록을 조회합니다.
     *
     * <트랜잭션>
     * - @Transactional(readOnly = true): 읽기 전용
     *
     * @param mno 회원 고유번호
     * @return 해당 회원의 티켓 payload(이미지 경로 등) 리스트
     *
     * <연계 컨트롤러 예시>
     * - GET /tickets/print?mno={mno}
     */
    @Transactional(readOnly = true)
    public List<String> findPayloads(int mno) {
        return ticketsMapper.findPayloads(mno);
    }//func end


    /**
     * 지난 경기(gno 목록) 기준으로 티켓을 '무효화' 처리합니다.
     * - tickets.valid 를 false(0)로 일괄 업데이트합니다.
     * - QR 문자열(ticket_code)은 보존되어 감사/추적에 유리합니다.
     *
     * <보안/운영 권장>
     * - 무효화 방식은 데이터 보존(증빙/분쟁 대비)에 유리
     *
     * @param expiredGno 지난 경기 gno 리스트
     */
    public void ticketNullify(List<Integer> expiredGno){
        String gnoList = expiredGno.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        ticketsMapper.ticketNullify(gnoList);
    }//func end
    /**
     * CSV 파일의 경기일자를 기준으로 '지난 경기'를 자동 식별하여
     * tickets.valid 값을 false(0)로 일괄 변경합니다.
     *
     * <동작 과정>
     * 1. FileService.getExpiredGames() 를 통해 지난 경기 gno 목록을 CSV에서 추출
     * 2. 지난 경기 목록이 존재할 경우, ticketNullify(List<Integer>) 를 호출하여 DB 업데이트
     * 3. 처리된 gno 개수를 반환
     *
     * @return 처리된 지난 경기(gno) 개수 (0이면 해당 없음)
     *
     * <연계 컨트롤러 예시>
     * - POST /tickets/nullify/csv
     *   Body: (없음)
     */
    @Transactional
    public int ticketNullifyCsv(){
        List<Integer>expired = fileService.getExpiredGames();
        if(expired.isEmpty()) return 0;
        ticketNullify(expired);
        return expired.size();
    }//func end

    /**
     * 지난 경기(gno 목록) 기준으로 티켓의 QR 문자열을 '삭제' 처리합니다.
     * - tickets.ticket_code 를 NULL로 일괄 업데이트합니다.
     * - 스캔 시 "만료된 QR코드입니다" 응답을 주도록 검증 로직과 함께 사용하세요.
     *
     * <보안/운영 주의>
     * - 문자열 삭제는 복구/추적에 불리할 수 있어, 보통 무효화(valid=false)와 병행 또는 대체 사용
     *
     * @param expiredGno 지난 경기 gno 리스트
     *
     * <연계 컨트롤러 예시>
     * - POST /tickets/delete
     *   Body(JSON): [1,2,3,4]
     */
    public void ticketDelete(List<Integer>expiredGno){
        String gnoList = expiredGno.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        ticketsMapper.ticketDelete(gnoList);
    }//func end
    @Transactional
    public int ticketDeleteCsv(){
        List<Integer>expired = fileService.getExpiredGames();
        if(expired.isEmpty()) return 0;
        ticketDelete(expired);
        return expired.size();
    }//func end

}//class end
