package com.example.demo.test.modbusTCP;

public class GgaParser {
    public static GgaPosition parse(String ggaSentence) {

        if (ggaSentence == null) {
            return null;
        }

        if (!ggaSentence.startsWith("$GNGGA")
                && !ggaSentence.startsWith("$GPGGA")) {
            return null;
        }

        String[] parts = ggaSentence.split(",");

        if (parts.length < 10) {
            return null;
        }

        GgaPosition position = new GgaPosition();

        position.setUtcTime(parts[1]);

        position.setLatitude(
                convertToDecimalDegrees(
                        parts[2],
                        parts[3],
                        true));

        position.setLongitude(
                convertToDecimalDegrees(
                        parts[4],
                        parts[5],
                        false));

        position.setFixQuality(
                Integer.parseInt(parts[6]));

        position.setSatellites(
                Integer.parseInt(parts[7]));

        position.setHdop(
                Double.parseDouble(parts[8]));

        position.setAltitude(
                Double.parseDouble(parts[9]));

        return position;
    }

    /**
     * NMEA坐标转十进制度
     *
     * 纬度:
     * 3019.54968900
     *
     * 经度:
     * 12004.47659460
     */
    private static double convertToDecimalDegrees(
            String value,
            String hemisphere,
            boolean latitude) {

        if (value == null || value.isBlank()) {
            return 0D;
        }

        int degreeLength = latitude ? 2 : 3;

        double degrees =
                Double.parseDouble(
                        value.substring(
                                0,
                                degreeLength));

        double minutes =
                Double.parseDouble(
                        value.substring(
                                degreeLength));

        double decimal =
                degrees + minutes / 60D;

        if ("S".equalsIgnoreCase(hemisphere)
                || "W".equalsIgnoreCase(hemisphere)) {

            decimal = -decimal;
        }

        return decimal;
    }
}
