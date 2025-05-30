package org.example.Service;

import org.example.Model.ShipModel;
import org.example.Dao.ShipDao;

public class Service {
    public static String LocationDbService(String Mmsi, double latitude, double longitude, String TrueHead, ShipDao shipDao ) {
        //파싱된 선박의 위치정보와 Mmsi 값을 DB에 저장

        ShipModel shipModel = new ShipModel();

        shipModel.setMmsi(Mmsi);
        shipModel.setLon(latitude);
        shipModel.setLat(longitude);
        shipModel.setTrueHead(Integer.parseInt(TrueHead));

        shipDao.ShipService(shipModel);

        return null;
    }
}
