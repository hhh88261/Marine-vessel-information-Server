package org.example.Decoder;

import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.sentence.SentenceException;
import dk.dma.ais.sentence.Vdm;
import org.example.Communication.Transmit;
import org.example.Dao.ShipDao;
import org.example.Model.ShipModel;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


import java.io.BufferedReader;
import java.io.IOException;


public class Decoder {

    private static ExternalApi externalApi;
    private static Transmit transmit;
    private static Calculatetor calculateLocation;
    public static String convertAisMessageToJson(AisMessage aisMessage) {
        return aisMessage.toString();
    }

    public static String AisDecoder(String response, BufferedReader reader, ShipDao shipDao) {
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
                        String[] shipInfo = externalApi.type5message(callSign, shipName);
                        String shipCallSign = shipInfo[0];
                        String ShipName = shipInfo[2];
                        String ShipCountry = shipInfo[3];
                        String ShipWeight = shipInfo[4];
                        String ShipOperationType = shipInfo[5];

                        //타입 5 파싱 메시지 전송
                        String message = msgId + "," + Mmsi5 + "," + shipName;
                        transmit.AisTransmit(message);
                        System.out.println("전송된 타입 5의 메시지: " + message);

                        //선박 제원 정보 전송
                        String infoType = "*";
                        String shipInfoMessage = infoType + "," + Mmsi5 + "," + shipCallSign + "," + ShipName + "," + ShipCountry + "," + ShipWeight + "," + ShipOperationType;
                        transmit.AisTransmit(shipInfoMessage);
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
                    String[] newLocation = calculateLocation.CalculateLocation(Pos, trueHead, Sog);

                    String newLat = newLocation[0];
                    String newLon = newLocation[1];

                    String caculatedLocation = newLat + "," + newLon;

                    String caculateLocation = msgId + "," + Pos + "," + Mmsi + "," + trueHead + "," + sogValue + "," + caculatedLocation + "," + Cog;

                    // updateMessage 메서드를 호출하여 소켓서버에 메시지 전송
                    transmit.AisTransmit(caculateLocation);
                    System.out.println("전송된 메시지:" + caculateLocation);

                    // trueHead의 값이 Null이거나 511일 때 예상 위치를 계산하는 메서드를 호출하지 않고 그대로 전송
                } else {
                    String Message = msgId + "," + Pos + "," + Mmsi + "," + Sog + "," + Cog;
                    System.out.println("전송된 메시지: " + Message);
                    transmit.AisTransmit(Message);
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}
