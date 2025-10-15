package phoenix.service;

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
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class ReservationExchangesService {
    private final ReservationExchangeMapper reservationExchangeMapper;
    private final ThreadPoolConfig threadPoolConfing;
    private final RedisService redisService;
    private final ReservationsService reservationsService;
    private final BaseballSocketHandler baseballSocketHandler;

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
        // redis 에 저장
        int saved = redisService.saveRequest(dto);
        if (saved == 0 || saved == 2) return saved;
        Executor executor = threadPoolConfing.changeExecutor();
        int fromSeat = reservationsService.reserveInfo(dto.getFrom_rno()).getSno();
        // 쓰레드풀에서 후속처리
        executor.execute( () -> { // 여기에 푸시알림 보낼메시지 작성해서 웹소켓에 보내기
            int mno = reservationsService.reserveInfo(dto.getTo_rno()).getMno();
            WebSocketSession session = baseballSocketHandler.getSession(mno);
            String msg = fromSeat + "번 좌석에서 좌석 교환 요청을 보냈습니다.";
            if(session != null && session.isOpen()){
                try{
                    session.sendMessage(new TextMessage(msg));
                    System.out.println("푸시알림발송 :" + msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }// try end
            }else { // 응답자가 서버에 접속이 안되있으면 redis에 저장
                redisService.saveMessage(mno,msg);
                System.out.println("응답자 미접속 Redis에 저장"+msg);
            }// if end
        });
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
        dto.setStatus("ACCEPTED");
        String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        dto.setResponded_at(nowTime);
        // 예매정보 조회
        ReservationsDto toDto = reservationsService.reserveInfo(dto.getTo_rno()); // 응답자 예매정보
        ReservationsDto fromDto = reservationsService.reserveInfo(from_rno); // 요청자 예매정보
        // db에저장
        reservationExchangeMapper.changeAdd(dto);
        // 예매좌석 교체
        reservationsService.reserveUpdate(fromDto.getSno(),toDto.getRno(),mno);         // 응답자 요청자 좌석으로 변경
        reservationsService.reserveUpdate(toDto.getSno(),from_rno,dto.getFrom_mno());   // 요청자 응답자 좌석으로 변경  *** 트랜잭션해야됨
        // redis 삭제
        redisService.deleteAllRequest(dto);
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
        return true;
    }// func end
}//func end
