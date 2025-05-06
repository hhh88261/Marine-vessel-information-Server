package org.example.Decoder;

public class Calculatetor {
    public static String[] CalculateLocation(String Pos, String cog, String Sog) {
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
}
