package phoenix.service; // 패키지명

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import phoenix.model.dto.ReservationExchangesDto;


@Service @RequiredArgsConstructor
@EnableScheduling
public class RedisService { // class start
    private final RedisTemplate<String,Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;




    public static Map< String , Object > requestMap = new HashMap<>();
    public static Map< String , List<Integer>> seatMap = new HashMap<>();
    public static Map<String,List<String>> alarmMap = new HashMap<>();


    /**
     * redis에 요청데이터 저장
     *
     * @param dto 요청Dto
     * @return int 성공 : 1 , 요청중인사람존재 : 2 , 요청자가 다른좌석에 요청중 : 0
     */
    public synchronized int saveRequest(ReservationExchangesDto dto) {
        // 1️⃣ Key 정의
        String requestKey = "change:request:" + dto.getFrom_rno(); // 요청자 기준 키
        String seatKey = "change:seat:" + dto.getTo_rno();         // 응답자 기준 키 (Hash)
        //RLock lock = redissonClient.getLock(seatKey);              // 응답자 단위 분산 락

        //try {
            // 2️⃣ 락 시도 (2초 대기, 5초 자동 해제)
            //if (!lock.tryLock(2, 5, TimeUnit.SECONDS)) return 2;

            // 3️⃣ 요청자 키 저장 (이미 존재하면 실패)
            //Boolean saved = redisTemplate.opsForValue()
            //        .setIfAbsent(requestKey, dto, 86400, TimeUnit.SECONDS); // serialize 제거, Jackson 사용
            //if (saved == null || !saved) return 0; // 이미 요청한 경우

            // 1️⃣ 기존 Hash에서 Set 가져오기
            //Set<Integer> seatSet = (Set<Integer>) redisTemplate.opsForHash().get(seatKey, "from_rnos");
            //if (seatSet == null) seatSet = new HashSet<>();
            //
            //// 2️⃣ 현재 요청자 추가
            //seatSet.add(dto.getFrom_rno());
            //
            //// 3️⃣ Hash에 Set으로 저장
            //String json = new ObjectMapper().writeValueAsString(seatSet);
            //redisTemplate.opsForHash().put(seatKey, "from_rnos", json);
            //
            //// TTL 설정 (24시간)
            //redisTemplate.expire(seatKey, 86400, TimeUnit.SECONDS);
            //
            //
            //// 요청자 기준 Key 확인
            //Object requestCheck = redisTemplate.opsForValue().get(requestKey);
            //if (requestCheck != null) {
            //    System.out.println(requestKey);
            //    System.out.println("✅ requestKey 저장 성공: " + requestCheck);
            //} else {
            //    System.out.println("❌ requestKey 저장 실패");
            //}
            //// 응답자 기준 Hash 확인
            //Map<Object, Object> seatHashCheck = redisTemplate.opsForHash().entries(seatKey);
            //if (seatHashCheck != null && !seatHashCheck.isEmpty()) {
            //    System.out.println(seatKey);
            //    System.out.println("✅ seatKey Hash 저장 성공: " + seatHashCheck);
            //} else {
            //    System.out.println("❌ seatKey Hash 저장 실패");
            //}
        if (requestMap.get(requestKey) != null || seatMap.containsValue(dto.getFrom_rno())) return 0;
        List<Integer> list = new ArrayList<>();
        list.add(dto.getFrom_rno());
        requestMap.put(requestKey,dto);
        seatMap.put(seatKey,list);

        return 1; // 성공

        //} catch (InterruptedException e) {
        //    Thread.currentThread().interrupt();
        //    return 2; // 락 실패 또는 인터럽트
        //} catch (Exception e) {
        //    e.printStackTrace();
        //    return 0; // 기타 실패
        //} finally {
        //    if (lock.isHeldByCurrentThread()) lock.unlock();
        //}
    }
    /**
     * redis에 요청데이터 조회
     *
     * @param from_rno 요청 예매번호
     * @return 요청 dto , 없으면 null
     */
    public ReservationExchangesDto getRequest(int from_rno){
        String key = "change:request:" + from_rno;
        return (ReservationExchangesDto) requestMap.get(key);
    }// func end

    /**
     * 로그인한 회원한테 온 요청목록 조회
     *
     * @param to_rno 응답자 예매번호
     * @return List<ReservationExchangesDto> 요청목록
     */
    public List<ReservationExchangesDto> getAllRequest(int to_rno){
        String key = "change:seat:" + to_rno;
        List<Integer> list = seatMap.get(key);
        if (list == null ) return null;
        List<ReservationExchangesDto> dtoList = new ArrayList<>();
        for (Integer i : list){
            String rekey = "change:request:"+i;
            ReservationExchangesDto reList = (ReservationExchangesDto) requestMap.get(rekey);
            dtoList.add(reList);
        }// for end
        return dtoList;

        //Map<Object, Object> json = redisTemplate.opsForHash().entries(key);
        //System.out.println("json = " + json);
        //if (json == null || json.isEmpty()) return Collections.emptyList();
        //List<ReservationExchangesDto> fromList = new ArrayList<>();
        //for (Object fromRnoObj : json.keySet()) {
        //    int fromRno = Integer.parseInt(fromRnoObj.toString());
        //    ReservationExchangesDto dto = (ReservationExchangesDto) redisTemplate.opsForValue()
        //            .get("change:request:" + fromRno);
        //    if (dto != null) fromList.add(dto);
        //}// for end
        //System.out.println("fromList = " + fromList);
        //return fromList;
    }// func end

    /**
     * redis에서 요청데이터 삭제
     *
     * @param from_rno 요청 예매번호
     */
    public void deleteRequest(int from_rno){
        String key = "change:request:" + from_rno;
        //redisTemplate.delete(key);
        requestMap.remove(key);
    }// func end

    /**
     * redis에서 응답예매번호에 대한 요청데이터 전체삭제
     *
     * @param dto
     */
    public void deleteAllRequest(ReservationExchangesDto dto){
        String seatKey = "change:seat:" + dto.getTo_rno();
        //Set<Object> fromRnos = redisTemplate.opsForSet().members(seatKey);
        //if (fromRnos != null && !fromRnos.isEmpty()) {
        //    for (Object fromRno : fromRnos) {
        //        String from_rno = String.valueOf(fromRno);
        //        redisTemplate.delete("change:request:" + from_rno);
        //    }// for end
        //    redisTemplate.delete(seatKey);
        //}//if end
        List<Integer> fromRnos = seatMap.get(seatKey);
        for (Integer i : fromRnos){
            String requestKey = "change:request:"+i;
            requestMap.remove(requestKey);
        }// for end
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
        //Long result = redisTemplate.opsForList().rightPush(key,message);
        //System.out.println("result = " + result);
        //boolean success = result != null && result > 0; // 정상 저장 여부
        //System.out.println("success = " + success);
        //redisTemplate.expire(key , Duration.ofHours(24));
        //List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        //System.out.println(key + list);
        List<String> alarmList = new ArrayList<>();
        alarmList.add(message);
        alarmMap.put(key , alarmList);
    }// func end

    /**
     * 알림 메시지 전체조회
     *
     * @param mno
     * @return List<String> 메시지 목록
     */
    public List<String> getMessage(int mno){
        String key = "alarm:" + mno;
        //List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        //System.out.println("list 알림전체조회 = " + list);
        //List<String> messages = list.stream().map(Object::toString).collect(Collectors.toList());
        //System.out.println("messages 알림메시지들 = " + messages);
        List<String> messages = alarmMap.get(key);
        return messages;
    }// func end

    /**
     * 알림 메시지 삭제
     *
     * @param mno
     */
    public void deleteMessage(int mno) {
        String key = "alarm:" + mno;
        //redisTemplate.delete(key);
        alarmMap.remove(key);
    }// func end

    /**
     * 저장시간 1일지난거 자정마다 삭제
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanUpMap(){
        LocalDateTime today = LocalDateTime.now();
        String seatKey = "change:seat:";
        String alarm = "alarm:";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Iterator<Map.Entry<String, Object>> iterator = requestMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            ReservationExchangesDto dto = (ReservationExchangesDto) entry.getValue();
            LocalDateTime saveTime = LocalDateTime.parse(dto.getRequested_at(), formatter);

            if (saveTime.plusDays(1).isBefore(today)) {
                // seatMap에서 삭제
                List<Integer> seatList = seatMap.get(seatKey + dto.getTo_rno());
                if (seatList != null) {
                    seatList.remove(Integer.valueOf(dto.getFrom_rno()));
                }// if end
                // alarmMap 에서 삭제
                List<String> strList = alarmMap.get(alarm+dto.getTo_rno());
                if (strList != null) {
                    strList.removeIf(str -> str.contains(String.valueOf(dto.getFromSeat())));
                }// if end
                // requestMap에서 안전하게 삭제
                iterator.remove();
            }// if end
        }// while end
    }// func end

}// class end
