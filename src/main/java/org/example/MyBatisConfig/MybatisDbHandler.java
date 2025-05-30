package org.example.MyBatisConfig;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.Decoder.AisDecoderService.Decoder;
import org.example.Communication.TcpReciver;
import org.example.Dao.ShipDao;
import java.io.IOException;
import java.io.Reader;

public class MybatisDbHandler {

    private Decoder decoder;

    private TcpReciver tcpReciver;

    public void MybatisDAO() throws IOException {
            // Configuration.xml 파일을 로드하여 db 연결 설정
            Reader reader = Resources.getResourceAsReader("Configuration.xml");
            // ShipDao는 SqlSesstionFactory를 통해
            // SqlSessionFactory : mybatis와 DB(Oracle) 서버를 연결
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            ShipDao shipDao = new ShipDao(sqlSessionFactory);
            tcpReciver.Reciver(shipDao);
    }
}
