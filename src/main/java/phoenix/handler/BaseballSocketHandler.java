package phoenix.handler; // 패키지명

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component // 빈 등록
public class BaseballSocketHandler extends TextWebSocketHandler { // class start

    // [1] 클라이언트와 서버의 연결이 시작되었을때
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("클라이언트와 연결 시작");
    }// func end

    // [2] 클라이언트와 서버의 연결이 종료되었을때
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("클라이언트와 연결 종료");
    }// func end

    // [3] 클라이언트가 서버에게 메시지를 보냈을때
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("클라이언트로부터 메시지 수신");
        System.out.println(" message = " + message);
    }// func end


    // * JSON 타입을 자바 타입으로 변환해주는 라이브러리 객체
    // .readValue( JSON형식, 변환할클래스명.class ) : 문자열(JSON) ---> 변환할클래스
    // .writeValueAsString( 변환될객체 ) : 변환될객체 ---> 문자열(JSON)
    private final ObjectMapper objectMapper = new ObjectMapper();
}// class end
