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

    @Test
    @DisplayName("Distance between the same points should be equal 0")
    void shouldReturnZeroForIdenticalPoints() {

        GeoPoint point = new GeoPoint(12.34, 23.45);

        HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();
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

        HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();
        double distanceWarsawLondon = calculator.calculateDistance(warsaw, london);
        double distanceLondonWarsaw = calculator.calculateDistance(london, warsaw);

        Assertions.assertThat(distanceWarsawLondon)
                .as("Spatial Symmetry")
                .isCloseTo(distanceLondonWarsaw, TOLERANCE);
    }

    @Test
    @DisplayName("Distance for antipodal points should be equal half of Earth circuit")
    void shouldHandleAntipodalPoints() {
        GeoPoint a = new GeoPoint(90.0, 0.0);
        GeoPoint b = new GeoPoint(-90.0, 0.0);
        double expectedDistance = Math.PI * EARTH_RADIUS_METERS;

        HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();
        double distanceBetwenAntipodalPoints = calculator.calculateDistance(a, b);

        Assertions.assertThat(distanceBetwenAntipodalPoints)
                .as("Distance between poles")
                .isCloseTo(expectedDistance, TOLERANCE);
    }

    @ParameterizedTest(name = "From {0},{1} to {2},{3} should be about {4} meters")
    @CsvSource({
            "52.2297, 21.0122, 51.5074, -0.1278, 1448835.0",   // Warsaw -> London
            "0.0, 0.0, 0.0, 1.0, 111195.0",                    // 1 degree at the equator
            "40.6397, -73.7789, 49.0097, 2.5479, 5836856.0"    // JFK (New York) -> CDG (Paris)
    })
    @DisplayName("Parameterized verification of known distances (relative to a sphere with R = 6,371 km)")
    void shouldCaluclateCorrectDistanceForKnownRoutes(double lat1, double lon1,
                                                      double lat2, double lon2,
                                                      double expectedDistance) {
        GeoPoint point1 = new GeoPoint(lat1, lon1);
        GeoPoint point2 = new GeoPoint(lat2, lon2);

        HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();
        double distance = calculator.calculateDistance(point1, point2);

        double acceptableError = expectedDistance * 0.005;

        Assertions.assertThat(distance)
                .as("Consistency with the spherical reference calculations")
                .isCloseTo(expectedDistance, Offset.offset(acceptableError));
    }
}