package phoenix.configuration; // 패키지명

import phoenix.handler.BaseballSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration // 빈 등록
@EnableWebSocket // 웹소켓 기능 활성화
@RequiredArgsConstructor
public class WebSocKetConfig implements WebSocketConfigurer { // class start
    // [*] 핸들러 불러오기
    private final BaseballSocketHandler baseballSocketHandler;

    // [1] 내가만든 소켓핸들러 등록
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler( baseballSocketHandler , "/socket" ).setAllowedOrigins("*");
    }// func end
}// class end
