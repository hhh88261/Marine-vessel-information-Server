package org.example.Util;

import org.example.Service.Service;

public final class CalculateUtil {
    private Service service;
    public static String CaculatePosUtil(String Pos) {
        // 현재 좌표 값을 계산
        String[] coordinates = Pos.split("/");
        double latitude = Double.parseDouble(coordinates[0]) / 600000.0;
        double longitude = Double.parseDouble(coordinates[1]) / 600000.0;
        String CaculatedPos = String.format("%.6f", latitude) + "," + String.format("%.6f", longitude);

        return CaculatedPos;
    }

    public static Double CaculateSogUtil(String Sog) {
        double sogValue = Double.parseDouble(Sog); // 문자열을 double로 변환
        sogValue = sogValue / 10.0; // 10으로 나누기
        return sogValue;
    }

    public static String[] CalculateLocation(String Pos, String cog, String Sog) {
        // lon과 lat에 저장, 형변환
        String[] coordinates = Pos.split("/");

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
