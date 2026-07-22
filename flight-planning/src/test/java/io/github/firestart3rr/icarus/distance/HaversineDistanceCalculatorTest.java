package io.github.firestart3rr.icarus.distance;

import io.github.firestart3rr.icarus.domain.GeoPoint;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.github.firestart3rr.icarus.domain.GeodeticConstants.EARTH_RADIUS_METERS;

class HaversineDistanceCalculatorTest {

    private static final Offset<Double> TOLERANCE = Offset.offset(0.1);

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    @DisplayName("Distance between the same points should be equal 0")
    void shouldReturnZeroForIdenticalPoints() {

        GeoPoint point = new GeoPoint(12.34, 23.45);

        double distance = calculator.calculateDistance(point, point);

        Assertions.assertThat(distance)
                .as("The distance of a point from itself")
                .isCloseTo(0.0, TOLERANCE);
    }

    @Test
    @DisplayName("Calculated distance should be symmetric: d(A,B) == d(B,A)")
    void shouldBeSymmetric() {
        GeoPoint warsaw = new GeoPoint(52.2297, 21.0122);
        GeoPoint london = new GeoPoint(51.5074, -0.1278);

        double distanceWarsawLondon = calculator.calculateDistance(warsaw, london);
        double distanceLondonWarsaw = calculator.calculateDistance(london, warsaw);

        Assertions.assertThat(distanceWarsawLondon)
                .as("Spatial Symmetry")
                .isCloseTo(distanceLondonWarsaw, TOLERANCE);
    }

    @ParameterizedTest
    @CsvSource({
            "90.0, 0.0, -90.0, 0.0, 1.0",
            "0.0, 0.0, 90.0, 00.0, 0.5",
            "0.0, 0.0, 0.0, 90.0, 0.5",
            "0.0, 0.0, 0.0, 180.0, 1.0",
    })
    @DisplayName("Analytically derived great-circle distances (fractions of circumference)")
    void shouldMatchAnalyticalDistancesForGreatCircleFractions(double lat1, double lon1,
                                     double lat2, double lon2,
                                     double partOfCircle) {
        GeoPoint a = new GeoPoint(lat1, lon1);
        GeoPoint b = new GeoPoint(lat2, lon2);
        double expectedDistance = Math.PI * partOfCircle * EARTH_RADIUS_METERS;

        double distanceBetweenAntipodalPoints = calculator.calculateDistance(a, b);

        Assertions.assertThat(distanceBetweenAntipodalPoints)
                .as("Distance between points on sphere")
                .isCloseTo(expectedDistance, TOLERANCE);
    }

    @ParameterizedTest(name = "From {0},{1} to {2},{3} should be about {4} meters")
    @CsvSource({
            "52.2297, 21.0122, 51.5074, -0.1278, 1448516.0209235002",   // Warsaw -> London
            "0.0, 0.0, 0.0, 1.0, 111195.0802335",                    // 1 degree at the equator
            "40.6397, -73.7789, 49.0097, 2.5479, 5833648.863205445"    // JFK (New York) -> CDG (Paris)
    })
    @DisplayName("Parameterized verification of known distances (relative to a sphere with R = 6,371 km)")
    void shouldCalculateCorrectDistanceForKnownRoutes(double lat1, double lon1,
                                                      double lat2, double lon2,
                                                      double expectedDistance) {
        GeoPoint point1 = new GeoPoint(lat1, lon1);
        GeoPoint point2 = new GeoPoint(lat2, lon2);

        double distance = calculator.calculateDistance(point1, point2);
        System.out.println("Distance: " + distance);

        double acceptableError = 0.0001;

        Assertions.assertThat(distance)
                .as("Consistency with the spherical reference calculations")
                .isCloseTo(expectedDistance, Offset.offset(acceptableError));
    }
}