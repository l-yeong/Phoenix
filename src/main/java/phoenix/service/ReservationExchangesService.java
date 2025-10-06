package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.configuration.RedisConfig;
import phoenix.configuration.ThreadPoolConfig;
import phoenix.model.dto.ReservationExchangesDto;
import phoenix.model.mapper.ReservationExchangeMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class ReservationExchangesService {
    private final ReservationExchangeMapper reservationExchangeMapper;
    private final ThreadPoolConfig threadPoolConfing;
    private final RedisConfig redisConfig;

    public boolean requestChange(ReservationExchangesDto dto){
        String rediskey = "exchange:request" + dto.getExno();
        Boolean check = redisConfig.redisTemplate().hasKey(rediskey);
        if (check != null && check){    // 이미 존재하는 요청인지 확인
            return false;
        }// if end
        // redis에 저장 , 유효시간 24시간
        redisConfig.redisTemplate().opsForValue().set(rediskey,dto, Duration.ofHours(24));
        Executor executor = threadPoolConfing.changeExecutor();
        executor.execute( () -> taskThread(dto) );
        return true;
    }// func end

    public void taskThread(ReservationExchangesDto dto){
        // 처리 로직
    }// func end
}//func end
