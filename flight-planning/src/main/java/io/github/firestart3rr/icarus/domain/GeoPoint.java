package io.github.firestart3rr.icarus.domain;

public record GeoPoint(double latitude, double longitude) {

    private static final double MIN_LATITUDE = -90.00;
    private static final double MAX_LATITUDE = 90.00;
    private static final double MIN_LONGITUDE = -180.00;
    private static final double MAX_LONGITUDE = 180.00;

    public GeoPoint {
        if (!(latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE)) {
            throw new IllegalArgumentException(
                    "Latitude must be in [" + MIN_LATITUDE + ", " + MAX_LATITUDE + "], got: " + latitude
            );
        }

        if (!(longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE)) {
            throw new IllegalArgumentException(
                    "Longitude must be in [" + MIN_LONGITUDE + ", " + MAX_LONGITUDE + "], got: " + longitude
            );
        }
    }
}
