package phoenix.service; // 패키지명

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
     * @return int 성공 : 1 , 요청중인사람존재 : 2 , 요청자가 다른좌석에 요청중 : 0
     */
    public int saveRequest(ReservationExchangesDto dto) {
        // 1️⃣ Key 정의
        String requestKey = "change:request:" + dto.getFrom_rno(); // 요청자 기준 키
        String seatKey = "change:seat:" + dto.getTo_rno();         // 응답자 기준 키 (Hash)
        RLock lock = redissonClient.getLock(seatKey);              // 응답자 단위 분산 락

        try {
            // 2️⃣ 락 시도 (2초 대기, 5초 자동 해제)
            if (!lock.tryLock(2, 5, TimeUnit.SECONDS)) return 2;

            // 3️⃣ 요청자 키 저장 (이미 존재하면 실패)
            Boolean saved = redisTemplate.opsForValue()
                    .setIfAbsent(requestKey, dto, 86400, TimeUnit.SECONDS); // serialize 제거, Jackson 사용
            if (saved == null || !saved) return 0; // 이미 요청한 경우

            // 4️⃣ 응답자 기준 Hash 가져오기
            String hashKey = seatKey;
            String field = String.valueOf(dto.getFrom_rno());

            Set<Integer> seatSet = (Set<Integer>) redisTemplate.opsForHash().get(hashKey, field);
            if (seatSet == null) seatSet = new HashSet<>();

            // 요청 좌석번호 추가
            seatSet.add(dto.getToSno());

            // Hash에 업데이트
            redisTemplate.opsForHash().put(hashKey, field, seatSet);

            // TTL 설정 (24시간)
            redisTemplate.expire(hashKey, 86400, TimeUnit.SECONDS);

            return 1; // 성공

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 2; // 락 실패 또는 인터럽트
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // 기타 실패
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
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
