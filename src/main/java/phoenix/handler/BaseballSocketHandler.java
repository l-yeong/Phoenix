package phoenix.handler; // 패키지명

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import phoenix.service.RedisService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component // 빈 등록
@RequiredArgsConstructor
public class BaseballSocketHandler extends TextWebSocketHandler { // class start
    private final RedisService redisService;

    // [*] 접속자 목록 ( key : 회원번호 , value : 접속자 정보 )
    private final ConcurrentHashMap<Integer, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // [1] 클라이언트와 서버의 연결이 시작되었을때
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("클라이언트와 연결 시작");
    }// func end

    // [2] 클라이언트와 서버의 연결이 종료되었을때
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("클라이언트와 연결 종료");
        // 세션 목록에서 제거 (userId가 세션 속성에 있어야 가능)
        Object userId = session.getAttributes().get("userId");
        if (userId != null) {
            int mno = (int) userId;
            sessions.remove(userId);
            System.out.println("세션 목록에서 제거, userId=" + mno);
        }// if end
    }// func end

    // [3] 클라이언트가 서버에게 메시지를 보냈을때
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("클라이언트로부터 메시지 수신");
        System.out.println(" message = " + message);
        // 메시지 json 으로 파싱
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String , Object> msg = objectMapper.readValue(message.getPayload(),Map.class);
        // 메시지 타입 확인
        String type = (String) msg.get("type");
        if ("login".equals(type)){
            // 클라이언트가 보낸 userId 가져오기(회원번호)
            int userId = (int) msg.get("mno");
            // 세션에 저장
            session.getAttributes().put("userId" , userId);
            sessions.put(userId, session);
            List<String> waitMessage = redisService.getMessage(userId);
            if (waitMessage != null && !waitMessage.isEmpty()){
                for (String alamMsg : waitMessage) {
                    session.sendMessage(new TextMessage(alamMsg));
                }// for end
                redisService.deleteMessage(userId);
            }// if end
        }// if end
    }// func end

    /**
     * 접속자 명단 상세 조회
     *
     * @param mno
     * @return WebSocketSession
     */
    public WebSocketSession getSession(int mno){
        return sessions.get(mno);
    }// func end

}// class end
