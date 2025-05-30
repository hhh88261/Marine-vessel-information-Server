package org.example.Communication;

import jakarta.websocket.*;

import java.net.URI;

public class Transmit {
    private String jsonInputString;
    private Session session;

    public void AisTransmit(String message) {
        connect("ws://localhost:7777/websocket/websocket");

        this.jsonInputString = message;
        if (session != null && session.isOpen()) {
            sendMessage(message);
        }
    }

    public void connect(String uri) {
        try {
            // WebSocket 클라이언트 역할을 수행할 수 있도록 컨테이너 생성
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            // 지정된 URI의 WebSocket 서버에 연결
            container.connectToServer(this, URI.create(uri));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("웹소켓 접속 성공: " + session.getId());
        sendMessage(jsonInputString);
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("서버로부터 수신된 메시지: " + message);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("웹소켓 연결 끊김: " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("ERROR!: " + throwable.getMessage());
    }

    private void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
            System.out.println("메시지 전송됨: " + message);
        } else {
            System.err.println("ERROR! 세션이 열려있지 않습니다.");
        }
    }
}
