package phoenix.service; // 패키지명

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
            String field = String.valueOf(dto.getFrom_rno());

            Set<Integer> seatSet = (Set<Integer>) redisTemplate.opsForHash().get(seatKey, field);
            if (seatSet == null) seatSet = new HashSet<>();

            // 요청 예매번호 추가
            seatSet.add(dto.getFrom_rno());
            System.out.println("seatSet = " + seatSet);

            // Hash에 업데이트
            redisTemplate.opsForHash().put(seatKey, field, seatSet);

            // TTL 설정 (24시간)
            redisTemplate.expire(seatKey, 86400, TimeUnit.SECONDS);
            // requestKey 확인
            Object request = redisTemplate.opsForValue().get("change:request:" + dto.getFrom_rno());
            System.out.println("request = " + request);

            // seatKey Hash 확인
            Map<Object, Object> seatHash = redisTemplate.opsForHash().entries("change:seat:" + dto.getTo_rno());
            System.out.println("seatHash = " + seatHash);


            String json = new ObjectMapper().writeValueAsString(seatSet);
            System.out.println(json); // "[30004]"
            // 요청자 기준 Key 확인
            Object requestCheck = redisTemplate.opsForValue().get(requestKey);
            if (requestCheck != null) {
                System.out.println(requestKey);
                System.out.println("✅ requestKey 저장 성공: " + requestCheck);
            } else {
                System.out.println("❌ requestKey 저장 실패");
            }
            // 응답자 기준 Hash 확인
            Map<Object, Object> seatHashCheck = redisTemplate.opsForHash().entries(seatKey);
            if (seatHashCheck != null && !seatHashCheck.isEmpty()) {
                System.out.println(seatKey);
                System.out.println("✅ seatKey Hash 저장 성공: " + seatHashCheck);
            } else {
                System.out.println("❌ seatKey Hash 저장 실패");
            }

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
        String key = "change:request:" + from_rno;
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
        Map<Object, Object> list = redisTemplate.opsForHash().entries(key);
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<ReservationExchangesDto> fromList = new ArrayList<>();
        for (Object fromRno : list.keySet()){
            String fromRnoStr = fromRno.toString();
            ReservationExchangesDto dto = (ReservationExchangesDto) redisTemplate.opsForValue().get("change:request:"+fromRnoStr);
            if (dto != null){
                fromList.add(dto);
            }// if end
        }// for end
        System.out.println("fromList = " + fromList);
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
        Long result = redisTemplate.opsForList().rightPush(key,message);
        System.out.println("result = " + result);
        boolean success = result != null && result > 0; // 정상 저장 여부
        System.out.println("success = " + success);
        redisTemplate.expire(key , Duration.ofHours(24));
        List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        System.out.println(key + list);
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
        System.out.println("list 알림전체조회 = " + list);
        List<String> messages = list.stream().map(Object::toString).collect(Collectors.toList());
        System.out.println("messages 알림메시지들 = " + messages);
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
