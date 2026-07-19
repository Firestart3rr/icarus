package io.github.firestart3rr.icarus.distance;

import io.github.firestart3rr.icarus.domain.GeoPoint;
import io.github.firestart3rr.icarus.domain.GeodeticConstants;

public class HaversineDistanceCalculator {

    public double calculateDistance(GeoPoint startPoint, GeoPoint endPoint) {
        double phi1 = Math.toRadians(startPoint.latitude());
        double phi2 = Math.toRadians(endPoint.latitude());

        double deltaPhi = Math.toRadians(endPoint.latitude() - startPoint.latitude());
        double deltaLambda = Math.toRadians(endPoint.longitude() - startPoint.longitude());

        double sinHalfPhi = Math.sin(deltaPhi / 2.0);
        double sinHalfLambda = Math.sin(deltaLambda / 2.0);

        double a = Math.pow(sinHalfPhi, 2) + Math.cos(phi1) * Math.cos(phi2) * Math.pow(sinHalfLambda, 2);

        a = Math.min(1.0, Math.max(0.0, a));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return GeodeticConstants.EARTH_RADIUS_METERS * c;
    }
}
