## Marine vessel information Server


## 리팩토링

단일 책임 원칙 준수 : 한 클래스가 담당하고 있던 여러 기능들을 분리

하드 코딩 제거 : 인터페이스로 추상화 하거나 Mock 개체 주입 가능하게 설계 변경

Async 예외 처리 : 예외가 발생했을 때 시스템은 계속 동작하되 클라이언트에게 에러 발생 메시지 전송

## 기능 분리

통신 계층   
- TcpReciver : 서버로 부터 메시지 수신 담당
- Transmit : 클라이언트에게 메시지 송신 담당
- WebSocket : 웹 소켓 연결과 엔드포인트 담당

디코딩 계층
- Calculator : 선박의 미래 항로 계산 담당
- Decoder : AIS 신호 디코딩
- ExternalApi : 해양수산부 API 연결 담당

DB 계층
- Dao : 가공된 데이터 저장 담당

Config 파일
- MyBatisDbHandler : Configuration.xml 파일을 로드하여 db 연결 담당

## 파티셔닝  
가설 :  
시간이 지날때 마다 테이블이 증가하고 인덱스 생성해야 하므로 디스크 부담 및 관리 비용 증가, 
다수의 사용자가 여러 테이블을 조회하게 된다면 처리량이 급증

case1 : 날짜별 테이블이 생성된 경우
case2 : Range 파티셔닝을 수행한 경우
