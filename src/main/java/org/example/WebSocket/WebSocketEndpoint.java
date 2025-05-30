package org.example.WebSocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.example.Util.MessageSpliter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 서버 엔드포인트
 * 클라이언트가 접속하면 세션을 저장하고, 메시지를 수신하여 다른 클라이언트에게 브로드캐스트함
 */
@ServerEndpoint("/websocket")
public class WebSocketEndpoint {

    private static final Set<Session> clients = new CopyOnWriteArraySet<>();
    private static final Set<Session> modalClients = new CopyOnWriteArraySet<>();
    private final MessageSpliter messageSpliter = new MessageSpliter();

    /**
     * 클라이언트가 웹소켓으로 연결될 때 호출됨
     */
    @OnOpen
    public void onOpen(Session session) {
        clients.add(session);
        System.out.println("웹소켓 연결: " + session.getId());
        try {
            session.getBasicRemote().sendText("웹소켓 접속 성공");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 메시지 수신
     */
    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        System.out.println("Message from " + session.getId() + ": " + message);
        if (message.startsWith("p")) {
            modalClients.add(session);
            System.out.println("modalClients: " + modalClients);
            messageSpliter.messageInputer(message);
        }
        // 모든 클라이언트에게 메시지를 브로드캐스트
        broadcast(message);
    }

    /**
     * 연결 종료
     */
    @OnClose
    public void onClose(Session session) {
        clients.remove(session);
        modalClients.remove(session);
        System.out.println("웹소켓 연결 종료: " + session.getId());
    }

    /**
     * 에러 헨들링
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("웹소켓 오류 [" + session.getId() + "]: " + throwable.getMessage());
    }

    /**
     * 연결된 모든 클라이언트에게 메시지를 브로드캐스트
     */
    private void broadcast(String message) {
        for (Session client : clients) {
            if (client.isOpen()) {
                try {
                    client.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    System.err.println("브로드캐스트 오류: " + e.getMessage());
                }
            }
        }
    }
}
