package org.example.Decoder.AisDecoderService;

import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.sentence.SentenceException;
import dk.dma.ais.sentence.Vdm;
import org.example.Communication.Transmit;
import org.example.Dao.ShipDao;
import org.example.Util.CalculateUtil;
import org.example.Communication.ExternalApi;
import org.example.Util.MessageBuilderUtil;
import org.example.Util.RemoveLetterUtil;

import java.io.BufferedReader;
import java.io.IOException;

public class Decoder {
    private static ExternalApi externalApi;
    private static Transmit transmit;
    private static CalculateUtil calculateLocation;
    private static Decoder1 decoder1;
    private static RemoveLetterUtil removeLetterUtil;
    private static MessageBuilderUtil messageBuilderUtil;
    public static String convertAisMessageToJson(AisMessage aisMessage) {
        return aisMessage.toString();
    }

    public static String AisDecoder(String response, BufferedReader reader, ShipDao shipDao) {
        try {
            String[] parts = response.split(",");

            // AIS 디코더 선언
            Vdm vdm = new Vdm();

            // ----------------msgId가 5일 경우---------------- \\
            if (parts.length > 1 && parts[5].startsWith("5")) {
                String rawMessagePart1 = response;

                // 다음 줄을 읽어옴
                while ((response = reader.readLine()) != null) {
                    String rawMessagePart2 = response;

                    // 두 개의 메시지를 순서대로 파싱
                    vdm.parse(rawMessagePart1);
                    vdm.parse(rawMessagePart2);

                    // 메시지 결합 후 파싱
                    AisMessage aisMessage = AisMessage.getInstance(vdm);
                    AisMessage5 type5Message = (AisMessage5) aisMessage;

                    // 파싱된 메시지에서 선박 이름, 호출 부호, IMO 번호 가져오기
                    int Mmsi5 = type5Message.getUserId();
                    int msgId = type5Message.getMsgId();
                    String shipName = type5Message.getName();
                    String callSign = type5Message.getCallsign();

                    // 불필요한 문자 제거
                    removeLetterUtil.cleanShipName(shipName);
                    removeLetterUtil.cleanCallSign(callSign);

                    // 해양수산부 API를 통해 조회한 데이터를 배열에 저장
                    String[] shipInfo = externalApi.type5message(callSign, shipName);
                    String shipCallSign = shipInfo[0];
                    String ShipName = shipInfo[2];
                    String ShipCountry = shipInfo[3];
                    String ShipWeight = shipInfo[4];
                    String ShipOperationType = shipInfo[5];

                    //제원정보와 Type 5 메시지 전송
                    transmit.AisTransmit(messageBuilderUtil.buildType5Message(msgId, Mmsi5, shipName));
                    transmit.AisTransmit(messageBuilderUtil.buildShipInfoMessage(Mmsi5, shipCallSign, ShipName, ShipCountry, ShipWeight, ShipOperationType));

                    if (response.startsWith("!AIVDM")) {
                        break;
                    }
                }
            } else {
                decoder1.AisDecoder1(response, shipDao);
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
