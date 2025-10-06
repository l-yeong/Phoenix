package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.configuration.RedisConfig;
import phoenix.configuration.ThreadPoolConfig;
import phoenix.model.dto.ReservationExchangesDto;
import phoenix.model.mapper.ReservationExchangeMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class ReservationExchangesService {
    private final ReservationExchangeMapper reservationExchangeMapper;
    private final ThreadPoolConfig threadPoolConfing;
    private final RedisService redisService;

    /**
     * 교환요청 접수
     *
     * @param dto 요청 Dto
     * @return true : 성공 , false : 중복요청
     */
    public boolean requestChange(ReservationExchangesDto dto){
        dto.setStatus("PENDING"); // 상태 : 대기
        String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        dto.setRequested_at(nowTime); // 요청시간 저장
        // redis 에 저장
        boolean saved = redisService.saveRequest(dto);
        if (!saved) return false;
        Executor executor = threadPoolConfing.changeExecutor();
        // 쓰레드풀에서 후속처리
        executor.execute( () -> {
            System.out.println("ThreadPool 처리 시작 : " + dto.getFrom_rno());
        });
        return true;
    }// func end

    /**
     * 응답자 요청 수락시 처리
     *
     * @param from_rno 요청 예매번호
     * @return true : 수락처리 , false : 요청 없음
     */
    public boolean acceptChange(int from_rno){
        ReservationExchangesDto dto = redisService.getRequest(from_rno);
        if (dto == null) return false;
        dto.setStatus("ACCEPTED");
        String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        dto.setResponded_at(nowTime);
        // db에저장

        // redis 삭제
        redisService.deleteRequest(from_rno);
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
