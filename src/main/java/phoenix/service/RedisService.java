package phoenix.service; // 패키지명

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import phoenix.configuration.RedisConfig;
import phoenix.model.dto.ReservationExchangesDto;

import java.time.Duration;
import java.util.Set;

@Service @RequiredArgsConstructor
public class RedisService { // class start
    private final RedisTemplate<String,Object> redisTemplate;

    /**
     * redis에 요청데이터 저장
     *
     * @param dto 요청Dto
     * @return ture : 저장성공 , false : 이미 존재
     */
    public boolean saveRequest(ReservationExchangesDto dto){
        String requestKey = "change:request:" + dto.getFrom_rno();  // 요청자 기준
        String seatKey = "change:seat:" + dto.getTo_rno();          // 응답좌석 기준 (index용)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(requestKey , dto , Duration.ofHours(24));
        if (success != null && success){
            redisTemplate.opsForSet().add(seatKey,String.valueOf(dto.getTo_rno()));
            redisTemplate.expire(seatKey,Duration.ofHours(24));
        }//if end
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
        return (ReservationExchangesDto) redisTemplate.opsForValue().get(key);
    }// func end

    /**
     * redis에서 요청데이터 삭제
     *
     * @param from_rno 요청 예매번호
     */
    public void deleteRequest(int from_rno){
        String key = "change:request:" + from_rno;
        redisTemplate.delete(key);
    }// func end

    /**
     * redis에서 응답예매번호에 대한 요청데이터 전체삭제
     *
     * @param dto
     */
    public void deleteAllRequest(ReservationExchangesDto dto){
        String seatKey = "change:seat:" + dto.getTo_rno();
        Set<Object> fromRnos = redisTemplate.opsForSet().members(seatKey);
        if (fromRnos != null && !fromRnos.isEmpty()) {
            for (Object fromRno : fromRnos) {
                String from_rno = String.valueOf(fromRno);
                redisTemplate.delete("change:request:" + from_rno);
            }// for end
            redisTemplate.delete(seatKey);
        }//if end
    }// func end
}// class end
