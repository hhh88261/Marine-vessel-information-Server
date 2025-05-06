package org.example.Communication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import org.example.AisMessageConnect;
import org.example.ApiExplorer;

public class Transmit {
    private String jsonInputString;

    private Session session;

    public static void AisTransmit(String message){
        AisMessageConnect client = new AisMessageConnect();
        client.connect("ws://localhost:7777/websocket/websocket");
        client.updateMessage(message);
    }

    public void updateMessage(String newMessage) {
        this.jsonInputString = newMessage;
        if (session != null && session.isOpen()) {
            sendMessage(newMessage);
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

    // WebSocket을 통해 메시지를 비동기적으로 전송
    private void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
            System.out.println("메시지 전송됨: " + message);
        } else {
            System.err.println("ERROR!");
        }
    }

}