package phoenix.service; // 패키지명

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import phoenix.configuration.RedisConfig;
import phoenix.model.dto.ReservationExchangesDto;

import java.time.Duration;

@Service @RequiredArgsConstructor
public class RedisService { // class start
    private final RedisConfig redisConfig;

    /**
     * redis에 요청데이터 저장
     *
     * @param dto 요청Dto
     * @return ture : 저장성공 , false : 이미 존재
     */
    public boolean saveRequest(ReservationExchangesDto dto){
        String key = "change:request:" + dto.getFrom_rno();
        Boolean success = redisConfig.redisTemplate().opsForValue().setIfAbsent(key , dto , Duration.ofHours(24));
        return success != null && success;
    }// func end

    /**
     * redis에 요청데이터 조회
     *
     * @param from_rno 요청 예매번호
     * @return 요청 dto , 없으면 null
     */
    public ReservationExchangesDto getRequest(int from_rno){
        String key = "change:request:" + from_rno;
        return (ReservationExchangesDto) redisConfig.redisTemplate().opsForValue().get(key);
    }// func end

    /**
     * redis에서 요청데이터 삭제
     *
     * @param from_rno 요청 예매번호
     */
    public void deleteRequest(int from_rno){
        String key = "change:request:" + from_rno;
        redisConfig.redisTemplate().delete(key);
    }// func end
}// class end
