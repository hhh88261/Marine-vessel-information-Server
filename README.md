## Marine vessel information Server

## 프로젝트 설명
- AIS 신호를 파싱하는 서버입니다.

## 요구 사항
- Java 21
- Maven 4.0.0

## 실행 가이드
```
git clone https://github.com/hhh88261/Marine-vessel-information-Server.git
cd Marine-vessel-information-Server
```

## 기능 

-
1. TCP 소켓과 웹 소켓 연결
```
try (Socket socket = new Socket()) {
  socket.connect(new InetSocketAddress("localhost", 9999));
  System.out.println("TCP소켓 접속 됨");

try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
  String response;
  AisMessageConnect client = new AisMessageConnect();
  client.connect("ws://localhost:7777/websocket/websocket");
```
2. dk.dma.ais.lib 의존성을 사용한 파싱
```
  vdm.parse(response);
  AisMessage aisMessage = AisMessage.getInstance(vdm);
  System.out.println(aisMessage);
  ```

3. 해양수산부 API 요청을 통한 선박의 제원 정보 조회
* API 정보 : https://www.data.go.kr/data/15055851/openapi.do (해양수산부 공개 API)
```
String jsonData = ApiExplorer.fetchJsonData(callSign, shipName);
```
```
HttpURLConnection conn = (HttpURLConnection) url.openConnection();

//GET 요청 수행
conn.setRequestMethod("GET");
conn.setRequestProperty("Content-type", "application/json");

//응답 출력
System.out.println("Response code: " + conn.getResponseCode());
```

4.추출된 데이터는 DB에 저장 
```
<insert id="insertShip" parameterType="org.example.Model.ShipModel">
INSERT INTO shiproute( MMSI, LON, LAT, SHIP_TIME, TRUE_HEAD)
VALUES ( #{mmsi, jdbcType=VARCHAR}, #{lon}, #{lat}, TO_CHAR(SYSDATE, 'HH24:MI:SS'), #{trueHead})
</insert>
```

