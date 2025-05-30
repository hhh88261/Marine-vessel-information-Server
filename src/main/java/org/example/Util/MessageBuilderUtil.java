package org.example.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageBuilderUtil {
    public static String buildType5Message(int msgId, int mmsi, String shipName) {
        return msgId + "," + mmsi + "," + shipName;
    }

    public static String buildShipInfoMessage(int mmsi, String callSign, String name, String country, String weight, String operationType) {
        return "*" + "," + mmsi + "," + callSign + "," + name + "," + country + "," + weight + "," + operationType;
    }

    public static Map<String, String> buildType1Message(String stringType1Message) {
        // msgId, pos, mmsi, trueHead, Sog, Cog;
        Map<String, String> map = new HashMap<>();
        stringType1Message = stringType1Message.substring(1, stringType1Message.length() - 1);
        stringType1Message = parsePos(stringType1Message);

        String[] pairs = stringType1Message.split(",\\s*");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");

            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, value);
            }
        }
        return map;
    }

    public static String parsePos(String stringType1Message) {

        String parseMessage = stringType1Message;

        Pattern pattern = Pattern.compile("pos=\\((\\d+),(\\d+)\\)\\s*=\\s*\\(\\d+,\\d+\\)");
        Matcher matcher = pattern.matcher(stringType1Message);

        if (matcher.find()) {
            String lon = matcher.group(1);
            String lat = matcher.group(2);
            String replacement = "pos=" + lon + "/" + lat;
            parseMessage = matcher.replaceFirst(replacement);
        }

        return parseMessage;
    }
}
