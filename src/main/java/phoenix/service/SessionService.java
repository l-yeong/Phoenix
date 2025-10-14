package phoenix.service; // 패키지명

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService { // class start
    private final ConcurrentHashMap<Integer, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(Integer userId, WebSocketSession session) {
        sessions.put(userId, session);
    }

    public void removeSession(Integer userId) {
        sessions.remove(userId);
    }

    public WebSocketSession getSession(Integer userId) {
        return sessions.get(userId);
    }

    public boolean isConnected(Integer userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }
}// class end
