package org.geysermc.connector.utils;

public class UUIDUtils {

    public static String toDashedUUID(String minifiedUuid) {
        return minifiedUuid.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");
    }

    public static String toMinifiedUUID(String dashedUuid) {
        return dashedUuid.replace("-", "");
    }
}
