package org.example.Util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.Dao.ShipDao;
// import org.example.Model.SelectShipModel;

import java.io.IOException;
import java.io.Reader;

public final class MessageSpliter {

    public void messageInputer(String PastMessage) throws IOException {
        /// db 연결
        Reader reader = Resources.getResourceAsReader("Configuration.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);

        ShipDao shipDao = new ShipDao(sqlSessionFactory);

        String[] splitMessage = PastMessage.split(",");

        System.out.println("과거 선박 정보: " + splitMessage[0] + splitMessage[1] + splitMessage[2]);
    }
}
