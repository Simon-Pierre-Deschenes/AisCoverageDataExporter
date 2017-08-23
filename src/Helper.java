/*
 * Minimalist implementation of the Helper class.
 */
public class Helper {
    private static double latSize = 0.04;//default is 0.0225225225;
    private static double lonSize = 0.05;//default is 0.0386812541;

    /**
     * latitude is rounded down longitude is rounded down. The id is lat-lon-coords representing bottom-left point in cell
     */
    public static String getCellId(double latitude, double longitude, int multiplicationFactor) {
        return roundLat(latitude, multiplicationFactor) + "_" + roundLon(longitude, multiplicationFactor);
    }

    public static double roundLat(double latitude, int multiplicationFactor) {
        double multiple = latSize * multiplicationFactor;
        return multiple * Math.floor(latitude / multiple);
    }

    public static double roundLon(double longitude, int multiplicationFactor) {
        double multiple = lonSize * multiplicationFactor;
        return multiple * Math.floor(longitude / multiple);
    }

    public static double getLatSize() {
        return latSize;
    }

    public static double getLonSize() {
        return lonSize;
    }
}