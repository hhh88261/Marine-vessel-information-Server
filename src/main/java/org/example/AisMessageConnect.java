package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.sentence.SentenceException;
import dk.dma.ais.sentence.Vdm;
import jakarta.websocket.*;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.Dao.ShipDao;
import org.example.Model.ShipModel;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.sql.Connection;


/**
 * AIS 메시지 타입별 구분
 * 타입 1(위치정보) : 선박 번호(MMSI), 위경도, 뱃머리 방향, 속도 포함
 * 타입 5(제원정보) : 선박 이름, 콜사인, 국적, 무게 등
 * 타입 5의 메시지는 바로 다음의 수신되는 메시지 까지 파싱하여야 함
 */

@ClientEndpoint
public class AisMessageConnect {
    private Session session;
    private volatile String jsonInputString;
    public Connection conn;

    public static String convertAisMessageToJson(AisMessage aisMessage) {
        return aisMessage.toString();
    }

    /**
     * Web Socket 서버에 연결하는 메서드
     */
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

    /**
     * AIS 서버로 부터 메시지를 수신하여 송신하고 웹 소켓 서버로 전송 및 DB에 정보를 저장하는 메서드
     * 내용
     * 1. AIS 서버와 연결
     * 2. 웹 소켓 서버와 연결
     * 3. 읽어온 데이터를 디코딩
     * 4. 디코됭 된 데이터를 DB에 저장
     * 5. 디코딩 된 데이터를 소켓 서버에 전송
     */
    public static String passingData(ShipDao shipDao) {
        try (Socket socket = new Socket()) {
            // AIS 신호를 전송하는 서버와 연결 후 TCP 통신으로 데이터 수신
            socket.connect(new InetSocketAddress("localhost", 9999));
            System.out.println("TCP소켓 접속 됨");

            // 받아온 데이터를 읽어와 reader에 저장
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String response;
                // 클라이언트에게 데이터를 전송하는 웹 소켓 서버에 접속
                AisMessageConnect client = new AisMessageConnect();
                client.connect("ws://localhost:7777/websocket/websocket");

                // reader의 데이터를 읽어옴
                while ((response = reader.readLine()) != null) {
                    try {
                        String[] parts = response.split(",");

                        // AIS 디코더 라이브러리 선언
                        Vdm vdm = new Vdm();
                        // ----------------msgId가 5일 경우---------------- \\
                        if (parts.length > 1 && parts[5].startsWith("5")) {
                            System.out.println("---타입 5의 메시지---");
                            String rawMessagePart1 = response;
                            System.out.println("첫번째 메시지 :" + rawMessagePart1);

                            // 다음 줄을 읽어옴
                            while ((response = reader.readLine()) != null) {
                                String rawMessagePart2 = response;
                                System.out.println("두번째 메시지 :" + rawMessagePart2);

                                // 두 개의 메시지를 순서대로 파싱
                                vdm.parse(rawMessagePart1);
                                vdm.parse(rawMessagePart2);

                                // 두 메시지를 결합하여 aisMessage에 저장
                                AisMessage aisMessage = AisMessage.getInstance(vdm);

                                // 메시지 타입이 5인지 확인 후 파싱
                                if (aisMessage instanceof AisMessage5) {
                                    AisMessage5 type5Message = (AisMessage5) aisMessage;
                                    System.out.println(type5Message);

                                    // 파싱된 메시지에서 선박 이름, 호출 부호, IMO 번호 가져오기
                                    int Mmsi5 = type5Message.getUserId();
                                    int msgId = type5Message.getMsgId();
                                    String shipName = type5Message.getName();
                                    String callSign = type5Message.getCallsign();

                                    // 불필요한 문자 제거
                                    shipName = shipName.replace("@", ""); // @ 기호 제거
                                    shipName = shipName.replaceAll("\\s+$", ""); // 마지막 공백 제거
                                    callSign = callSign.replaceAll("\\s+", ""); // 모든 공백 제거

                                    // 해양수산부 API를 통해 조회한 데이터를 배열에 저장
                                    String[] shipInfo = type5message(callSign, shipName);
                                    String shipCallSign = shipInfo[0];
                                    String ShipName = shipInfo[2];
                                    String ShipCountry = shipInfo[3];
                                    String ShipWeight = shipInfo[4];
                                    String ShipOperationType = shipInfo[5];

                                    //타입 5 파싱 메시지 전송
                                    String message = msgId + "," + Mmsi5 + "," + shipName;
                                    client.updateMessage(message);
                                    System.out.println("전송된 타입 5의 메시지: " + message);

                                    //선박 제원 정보 전송
                                    String infoType = "*";
                                    String shipInfoMessage = infoType + "," + Mmsi5 + "," + shipCallSign + "," + ShipName + "," + ShipCountry + "," + ShipWeight + "," + ShipOperationType;
                                    client.updateMessage(shipInfoMessage);
                                    System.out.println("전송된 선박 제원정보: " + shipInfoMessage);
                                } else {
                                    System.out.println("The message is not of type 5.");
                                }
                                if (response.startsWith("!AIVDM")) {
                                    break;
                                }
                            }
                        }
                        // ---- msgId가 5가 아닌 AIS 메시지를 파싱 ---- \\

                        // vdm 객체에 전달하여 파싱 후 aisMessage에 저장
                        vdm.parse(response);
                        AisMessage aisMessage = AisMessage.getInstance(vdm);
                        System.out.println(aisMessage);

                        // 파싱된 메시지를 JSON으로 변환하고 저장
                        String jsonMessage = convertAisMessageToJson(aisMessage);

                        String MsgIdRegex = "msgId=(\\d+)";
                        String PosRegex = "pos=\\(([^)]+)\\)";
                        String MmsiRegex = "userId=(\\d+)";
                        String trueHeadRegex = "trueHeading=(\\d+)";
                        String SogRegex = "sog=(\\d+)";
                        String CogRegex = "cog=(\\d+)";

                        Pattern MsgIdpattern = Pattern.compile(MsgIdRegex);
                        Matcher MsgIdMatcher = MsgIdpattern.matcher(jsonMessage);

                        Pattern Pospattern = Pattern.compile(PosRegex);
                        Matcher PosMatcher = Pospattern.matcher(jsonMessage);

                        Pattern Mmsipattern = Pattern.compile(MmsiRegex);
                        Matcher MmsiMatcher = Mmsipattern.matcher(jsonMessage);

                        Pattern TrueHeadpattern = Pattern.compile(trueHeadRegex);
                        Matcher TrueHeadMatcher = TrueHeadpattern.matcher(jsonMessage);

                        Pattern Sogpattern = Pattern.compile(SogRegex);
                        Matcher SogMatcher = Sogpattern.matcher(jsonMessage);

                        Pattern Cogpattern = Pattern.compile(CogRegex);
                        Matcher CogMatcher = Cogpattern.matcher(jsonMessage);

                        String msgId = null;
                        String pos = null;
                        String mmsi = null;
                        String trueHead = null;
                        String Sog = null;
                        String Cog = null;

                        if (PosMatcher.find() && MmsiMatcher.find() && MsgIdMatcher.find() && TrueHeadMatcher.find() && SogMatcher.find() && CogMatcher.find()) {
                            msgId = MsgIdMatcher.group(1);
                            System.out.println("추출된 메시지 타입: " + msgId);

                            pos = PosMatcher.group(1);
                            System.out.println("추출된 위경도: " + pos);

                            mmsi = MmsiMatcher.group(1);
                            System.out.println("추출된 mmsi: " + mmsi);

                            trueHead = TrueHeadMatcher.group(1);
                            System.out.println("추출된 TrueHeading: " + trueHead);

                            Sog = SogMatcher.group(1);
                            System.out.println("추출된 Sog: " + Sog);

                            Cog = SogMatcher.group(1);
                            System.out.println("추출된 Cog: " + Cog);

                            double sogValue = Double.parseDouble(Sog); // 문자열을 double로 변환
                            sogValue = sogValue / 10.0; // 10으로 나누기

                            // 현재 좌표 값을 계산 후 Pos에 저장, mmsi와 truehead는 포멧하여 저장
                            String[] coordinates = pos.split(",");
                            double latitude = Double.parseDouble(coordinates[0]) / 600000.0;
                            double longitude = Double.parseDouble(coordinates[1]) / 600000.0;
                            String Pos = String.format("%.6f", latitude) + "," + String.format("%.6f", longitude);
                            String Mmsi = String.format(mmsi);
                            int TrueHead = Integer.parseInt(trueHead);

                            //파싱된 선박의 위치정보와 Mmsi 값을 DB에 저장
                            if (Mmsi != null && Pos != null) {
                                ShipModel shipModel = new ShipModel();

                                shipModel.setMmsi(Mmsi);
                                shipModel.setLon(latitude);
                                shipModel.setLat(longitude);
                                shipModel.setTrueHead(TrueHead);

                                shipDao.ShipService(shipModel);
                            }

                            // trueHead값이 null이거나 511이 아닐 때 예상 위치를 계산하는 메서드를 호출하여 계산하고 그 값을 소켓서버에 전송
                            if (Pos != null && trueHead != null && trueHead != "511" && Sog != null) {
                                // calculateLocation 호출하여 새로운 위치 계산
                                String[] newLocation = calculateLocation(Pos, trueHead, Sog);

                                String newLat = newLocation[0];
                                String newLon = newLocation[1];

                                String caculatedLocation = newLat + "," + newLon;

                                String caculateLocation = msgId + "," + Pos + "," + Mmsi + "," + trueHead + "," + sogValue + "," + caculatedLocation + "," + Cog;

                                // updateMessage 메서드를 호출하여 소켓서버에 메시지 전송
                                client.updateMessage(caculateLocation);
                                System.out.println("전송된 메시지:" + caculateLocation);

                                // trueHead의 값이 Null이거나 511일 때 예상 위치를 계산하는 메서드를 호출하지 않고 그대로 전송
                            } else {
                                String Message = msgId + "," + Pos + "," + Mmsi + "," + Sog + "," + Cog;
                                System.out.println("전송된 메시지: " + Message);
                                client.updateMessage(Message);
                            }
                        } else {
                            System.out.println("위경도 값 또는 MMSI가 없음");
                        }
                    } catch (AisMessageException e) {
                        System.err.println("유효하지 않은 메시지 ID 입니다: " + e.getMessage());
                    } catch (SentenceException e) {
                        System.err.println("Sentence error: " + e.getMessage());
                    } catch (SixbitException e) {
                        System.err.println("Sixbit error: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 선박의 미래 위치를 계산하는 메서드
     */
    public static String[] calculateLocation(String Pos, String cog, String Sog) {
        // lon과 lat에 저장, 형변환
        Pos = Pos.replace("(", "").replace(")", "");
        String[] coordinates = Pos.split(",");

        double lat = Double.parseDouble(coordinates[0].trim());
        double lon = Double.parseDouble(coordinates[1].trim());

        int cogCalucalate = Integer.parseInt(cog);
        double SogCalucalate = Double.parseDouble(Sog) / 10.0;
        System.out.println(SogCalucalate);// Sog를 double로

        // 10분 후 이동 거리 계산
        double d = (SogCalucalate * 10.0 / 60.0); // 10분 이동 거리

        // 이동 거리 계산
        double deltaNorth = d * Math.cos(Math.toRadians(cogCalucalate)); // 북쪽 이동 거리
        double deltaEast = d * Math.sin(Math.toRadians(cogCalucalate));  // 동쪽 이동 거리

        // 위도 변화
        double deltaLat = deltaNorth / 110.574; // 위도 1km당 변화량

        // 경도 변화
        double deltaLon = deltaEast / (111.320 * Math.cos(Math.toRadians(lat))); // 경도 1km당 변화량

        // 새로운 위치 계산
        String newLat = String.format("%.6f", lat + deltaLat);
        String newLon = String.format("%.6f", lon + deltaLon);

        return new String[]{newLat, newLon};
    }

    /** 해양수산부_선박제원정보 서비스 API를 호출하는 메서드
     * 타입 5의 메시지가 들어오면 callSign과 ShipName을 받아 선박의 제원정보를 조회
     */
    public static String[] type5message(String callSign, String shipName) throws Exception {
        // 선박제원정보 가져오기
        String jsonData = ApiExplorer.fetchJsonData(callSign, shipName);

        // JSON 문자열을 JsonNode로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonData);

        // "item" 노드로 이동하여 원하는 필드 추출
        JsonNode itemNode = jsonNode.path("body").path("items").path("item");

        // 추출할 필드들
        String clsgn = itemNode.path("clsgn").asText();      // 호출부호
        String vsslNo = itemNode.path("vsslNo").asText();    // 선박번호
        String vsslEngNm = itemNode.path("vsslEngNm").asText();  // 선박명
        String vsslNlty = itemNode.path("vsslNlty").asText();    // 선박국적
        String grtg = itemNode.path("grtg").asText();        // 총톤수
        String nvgShapNm = itemNode.path("nvgShapNm").asText();  // 운항형태명

        // 추출한 값을 배열에 저장
        String[] shipInfo = {clsgn, vsslNo, vsslEngNm, vsslNlty, grtg, nvgShapNm};

        return shipInfo;
    }

    /**
     * 웹 소켓 서버에 Json 메시지를 전송하는 메서드
     */
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