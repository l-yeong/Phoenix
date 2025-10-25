package phoenix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import phoenix.configuration.ThreadPoolConfig;
import phoenix.handler.BaseballSocketHandler;
import phoenix.model.dto.ReservationExchangesDto;
import phoenix.model.dto.ReservationsDto;
import phoenix.model.mapper.ReservationExchangeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class ReservationExchangesService {
    private final ReservationExchangeMapper reservationExchangeMapper;
    private final ThreadPoolConfig threadPoolConfing;
    private final RedisService redisService;
    private final ReservationsService reservationsService;
    private final BaseballSocketHandler baseballSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * 교환요청 접수
     *
     * @param dto 요청 Dto
     * @return int 성공 : 1 , 요청중인사람존재 : 2 , 요청자가 다른좌석에 요청중 : 0
     */
    public int requestChange(ReservationExchangesDto dto){
        dto.setStatus("PENDING"); // 상태 : 대기
        String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        dto.setRequested_at(nowTime); // 요청시간 저장
        System.out.println("dto = " + dto);
        ReservationsDto fromDto = (ReservationsDto) reservationsService.reserveInfo(dto.getFrom_rno()).get("reservation");
        int fromSeat = fromDto.getSno();
        dto.setFromSeat(fromSeat);
        // redis 에 저장
        int saved = redisService.saveRequest(dto);
        System.out.println("saved = " + saved);
        if (saved == 0 || saved == 2) return saved;
        if (saved == 1) {
            Executor executor = threadPoolConfing.changeExecutor();
            // 쓰레드풀에서 후속처리
            executor.execute(() -> { // 여기에 푸시알림 보낼메시지 작성해서 웹소켓에 보내기
                HashMap<String, Object> map = (HashMap<String, Object>) reservationsService.reserveInfo(dto.getTo_rno());
                System.out.println("map = " + map);
                ReservationsDto toDto = (ReservationsDto) map.get("reservation");
                System.out.println("toDto = " + toDto);
                int mno = toDto.getMno();
                System.out.println("mno = " + mno);
                WebSocketSession session = baseballSocketHandler.getSession(mno);
                String msg = fromSeat + "번 좌석에서 좌석 교환 요청을 보냈습니다.";
                System.out.println("msg = " + msg);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(msg));
                        System.out.println("푸시알림발송 :" + msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }// try end
                } else { // 응답자가 서버에 접속이 안되있으면 redis에 저장
                    redisService.saveMessage(mno, msg);
                    System.out.println("응답자 미접속 Redis에 저장" + msg);
                }// if end
            });
        }// if end
        return saved;
    }// func end

    /**
     * 응답자 요청 수락시 처리
     *
     * @param from_rno 요청 예매번호
     * @return true : 수락처리 , false : 요청 없음
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptChange(int mno ,int from_rno){
        ReservationExchangesDto dto = redisService.getRequest(from_rno);
        if (dto == null) return false;
        dto.setStatus("approved");
        String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        dto.setResponded_at(nowTime);
        // db에저장
        boolean ch = reservationExchangeMapper.changeAdd(dto);
        System.out.println("ch = " + ch);
        // 예매좌석 교체
        boolean ch1 = reservationsService.reserveUpdate(dto.getFromSeat(), dto.getTo_rno(), mno);         // 응답자 요청자 좌석으로 변경
        System.out.println("ch1 = " + ch1);
        boolean ch2 = reservationsService.reserveUpdate(dto.getToSno(),from_rno,dto.getFrom_mno());   // 요청자 응답자 좌석으로 변경  *** 트랜잭션해야됨
        System.out.println("ch2 = " + ch2);
        // redis 삭제
        redisService.deleteAllRequest(dto);
        String key = "change:seat:"+dto.getTo_rno();
        List<Integer> list = redisService.seatMap.get(key);
        list.removeIf(i -> i == from_rno);
        return true;
    }// func end

    /**
     * 응답자 요청 거절시 처리
     *
     * @param from_rno 요청 예매번호
     * @return true : 거절처리 , false : 요청 없음
     */
    public boolean rejectChange(int from_rno){
        ReservationExchangesDto dto = redisService.getRequest(from_rno);
        if (dto == null) return false;
        String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        dto.setResponded_at(nowTime);
        redisService.deleteRequest(from_rno); // redis 삭제
        String key = "change:seat:"+dto.getTo_rno();
        List<Integer> list = redisService.seatMap.get(key);
        list.removeIf(i -> i == from_rno);
        return true;
    }// func end

    /**
     * 알림메시지 발송
     *
     * @param mno
     */
    public void responseMessage( int mno , String msg ){
        Executor executor = threadPoolConfing.changeExecutor();
        executor.execute( () -> {
            try{
                WebSocketSession session = baseballSocketHandler.getSession(mno);
                if (session != null && session.isOpen()){
                    session.sendMessage(new TextMessage(msg));
                    System.out.println("접속 = " + msg);
                }else { // 요청자가 접속 안되어 있을때 redis에 메시지 저장
                    redisService.saveMessage(mno , msg);
                    System.out.println("미접속 = " + msg);
                }// if end
            } catch (Exception e) {
                e.printStackTrace();
            }// try end
        }); // thread end
    }// func end


}// class end
