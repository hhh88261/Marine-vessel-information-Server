package org.example.Util;

public final class RemoveLetterUtil {
    public static String cleanShipName(String name){
        return name.replace("@","").replaceAll("\\s+$","");
    }

    public static String cleanCallSign(String callSign){
        return callSign.replaceAll("\\s+","");
    }
}
