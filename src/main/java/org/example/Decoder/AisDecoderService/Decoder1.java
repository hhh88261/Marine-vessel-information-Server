package org.example.Decoder.AisDecoderService;

import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.sentence.SentenceException;
import org.example.Communication.Transmit;
import org.example.Dao.ShipDao;
import org.example.Service.Service;
import org.example.Util.CalculateUtil;
import org.example.Communication.ExternalApi;
import dk.dma.ais.sentence.Vdm;
import org.example.Util.MessageBuilderUtil;

import java.util.Map;

import static org.example.Util.MessageBuilderUtil.buildType1Message;

public class Decoder1 {
    private static ExternalApi externalApi;
    private static Transmit transmit;
    private static CalculateUtil calculateUtil;
    private static MessageBuilderUtil messageBuilderUtil;
    private static Service service;

    public static String AisDecoder1(String response, ShipDao shipDao) throws SentenceException, AisMessageException, SixbitException {

        // AIS 디코더 라이브러리 선언
        Vdm vdm = new Vdm();

        // 메시지 파싱
        vdm.parse(response);
        AisMessage aisMessage = AisMessage.getInstance(vdm);
        String StringType1Message = aisMessage.toString();
        System.out.println("Type 1 AIS 메시지 : " + StringType1Message);

        // 필요 요소 추출
        Map<String, String> parsed = buildType1Message(StringType1Message);

        String msgId = parsed.get("msgId");
        String Pos = parsed.get("pos");
        String Mmsi = parsed.get("userId");
        String TrueHead = parsed.get("trueHeading");
        String Sog = parsed.get("sog");
        String Cog = parsed.get("cog");

        // Sog(선박 속도) 계산
        double sogValue = CalculateUtil.CaculateSogUtil(Sog);

        // Pos(위경도) 계산
        String pos = calculateUtil.CaculatePosUtil(Pos);

        String[] position = pos.split(",");
        Double latitude = Double.parseDouble(position[0]);
        Double longitude = Double.parseDouble(position[1]);

        // 추출된 값 DB 저장
        service.LocationDbService(Mmsi, latitude, longitude, TrueHead, shipDao);

        // trueHead값이 null이거나 511이 아닐 때 예상 위치를 계산하는 메서드를 호출하여 계산하고 그 값을 소켓서버에 전송
        if (TrueHead != null && !TrueHead.equals("511")) {
            // 미래의 선박 위치 계산
            String[] newLocation = calculateUtil.CalculateLocation(Pos, TrueHead, Sog);

            String newLat = newLocation[0];
            String newLon = newLocation[1];

            // 미래 선박위치 + 현재 선박위치가 포함된 데이터
            String caculateLocation = msgId + "," + latitude + "," + longitude + "," + Mmsi + "," + TrueHead + "," + sogValue + "," + newLat + "," + newLon + "," + Cog;

            // 소켓서버에 전송
            transmit.AisTransmit(caculateLocation);
        } else {
            String Message = msgId + "," + latitude + "," + longitude + "," + Mmsi + "," + Sog + "," + Cog;
            transmit.AisTransmit(Message);
        }
        return response;
    }
}
