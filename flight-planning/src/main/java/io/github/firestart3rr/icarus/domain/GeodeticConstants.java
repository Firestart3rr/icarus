package io.github.firestart3rr.icarus.domain;

public final class GeodeticConstants {

    private GeodeticConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final double EARTH_RADIUS_METERS = 6371008.8;  //average arithmetical radius in WGS84
}
