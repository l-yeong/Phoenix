package phoenix.service; // 패키지명

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import phoenix.model.dto.ReservationExchangesDto;


@Service @RequiredArgsConstructor
public class RedisService { // class start
    private final RedisTemplate<String,Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    /**
     * redis에 요청데이터 저장
     *
     * @param dto 요청Dto
     * @return ture : 저장성공 , false : 이미 존재 or 실패
     */
    public boolean saveRequest(ReservationExchangesDto dto){
        String requestKey = "change:request:" + dto.getFrom_rno();  // 요청자 기준
        String seatKey = "change:seat:" + dto.getTo_rno();          // 응답좌석 기준 (index용)
        RLock lock = redissonClient.getLock(seatKey);               // 응답자 단위 분산 락 생성
        try{// 2초안에 락 못잡으면 실패 , 5초뒤에는 자동으로 락 해제
            if (lock.tryLock(2,5, TimeUnit.SECONDS)){
                /*
                * lua Script 내용
                *
                *   -- KEYS[1] : 요청자 기준 키 (requestKey)
                    -- KEYS[2] : 좌석 기준 키 (seatKey)
                    -- ARGV[1] : 직렬화된 DTO 데이터
                    -- ARGV[2] : TTL(초)
                    -- ARGV[3] : 좌석번호 (Set에 추가할 값)
                */
                String lua = """
                            -- 요청자 기준 키가 없으면 새로 저장
                            if redis.call('SETNX', KEYS[1], ARGV[1] ) == 1 then
                                -- TTL 설정(24시간)
                                redis.call('EXPIRE' , KEYS[1] , ARGV[2] )
                                -- 응답자 기준 세트에 응답자 예매번호 추가
                                redis.call('SADD', KEYS[2], ARGV[3])
                                -- 응답자 세트에도 TTL 설정
                                redis.call('EXPIRE', KEYS[2], ARGV[2])
                                return 1
                            else
                                return 0
                            end
                        """;
                Long result = redisTemplate.execute(
                        new DefaultRedisScript<>(lua, Long.class),
                        List.of(requestKey,seatKey),
                        serialize(dto),                    // ARGV[1] : 요청 데이터(JSON)
                        String.valueOf(86400),          // ARGV[2] : TTL 24시간
                        String.valueOf(dto.getTo_rno())   // ARGV[3] : 응답자 예매번호
                );
                // 결과반환
                return result != null && result == 1;
            }else { return false; }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            if (lock.isHeldByCurrentThread()){
                lock.unlock();
            }// inf end
        }// try end
    }// func end

    /**
     * redis에 요청데이터 조회
     *
     * @param from_rno 요청 예매번호
     * @return 요청 dto , 없으면 null
     */
    public ReservationExchangesDto getRequest(int from_rno){
        String key = "change:seat:" + from_rno;
        return (ReservationExchangesDto) redisTemplate.opsForValue().get(key);
    }// func end

    /**
     * 로그인한 회원한테 온 요청목록 조회
     *
     * @param to_rno 응답자 예매번호
     * @return List<ReservationExchangesDto> 요청목록
     */
    public List<ReservationExchangesDto> getAllRequest(int to_rno){
        String key = "change:seat:" + to_rno;
        Set<Object> list = redisTemplate.opsForSet().members(key);
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<ReservationExchangesDto> fromList = new ArrayList<>();
        for (Object fromRno : list){
            ReservationExchangesDto dto = (ReservationExchangesDto) redisTemplate.opsForValue().get("change:request:"+fromRno);
            if (dto != null){
                fromList.add(dto);
            }// if end
        }// for end
        return fromList;
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

    /**
     * DTO를 JSON 문자열로 직렬화
     *
     * @param dto
     * @return String 직렬화된 dto
     */
    public String serialize(ReservationExchangesDto dto){
        try{
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("DTO 직렬화 실패",e);
        }// try end
    }// func end

    /**
     * 알림 저장
     *
     * @param mno
     * @param message
     */
    public void saveMessage(int mno , String message){
        String key = "alarm:" + mno;
        redisTemplate.opsForList().rightPush(key,message);
        redisTemplate.expire(key , Duration.ofHours(24));
    }// func end

    /**
     * 알림 메시지 전체조회
     *
     * @param mno
     * @return List<String> 메시지 목록
     */
    public List<String> getMessage(int mno){
        String key = "alarm:" + mno;
        List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        List<String> messages = list.stream().map(Object::toString).collect(Collectors.toList());
        return messages;
    }// func end

    /**
     * 알림 메시지 삭제
     *
     * @param mno
     */
    public void deleteMessage(int mno) {
        String key = "alarm:" + mno;
        redisTemplate.delete(key);
    }// func end

}// class end
