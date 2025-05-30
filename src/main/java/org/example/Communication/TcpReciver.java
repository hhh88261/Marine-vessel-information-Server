package org.example.Communication;
import org.example.Decoder.AisDecoderService.Decoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.example.Dao.ShipDao;

public class TcpReciver {
    private Decoder decoder;
    public static String Reciver(ShipDao shipDao) {
        Decoder decoder = new Decoder(); // 또는 생성자를 통해 주입

        try (Socket socket = new Socket()) {
            // AIS 신호를 전송하는 서버와 연결 후 TCP 통신으로 데이터 수신
            socket.connect(new InetSocketAddress("localhost", 9999));
            System.out.println("TCP소켓 접속 됨");

            // 받아온 데이터를 읽어와 reader에 저장
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String response;

                // reader의 데이터를 읽어옴
                while ((response = reader.readLine()) != null) {
                    decoder.AisDecoder(response, reader, shipDao);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
};