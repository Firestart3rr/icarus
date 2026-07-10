package io.github.firestart3rr.icarus.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class GeoPointTest {

    @Test
    void shouldCreatePointWhenCoordinatesAreWithinRange() {
        //given
        double latitude = 45.67;
        double longitude = 12.34;

        //when
        GeoPoint geoPoint = assertDoesNotThrow(() -> new GeoPoint(latitude, longitude));

        //then
        assertEquals(latitude, geoPoint.latitude());
        assertEquals(longitude, geoPoint.longitude());
    }

    @ParameterizedTest
    @CsvSource({
            "-90.00, 10.00",
            "90.00, 10.00",
            "10.00, -180.00",
            "10.00, 180.00"
    })
    void shouldCreatePointWhenCoordinatesAreBoundaryValuesWithinRange(double latitude, double longitude) {
        assertDoesNotThrow(() -> new GeoPoint(latitude, longitude));
    }

    @ParameterizedTest
    @CsvSource({
            "-902.00, 10.00",
            "90.00, 1530.00",
            "10.00, -1860.00",
            "1042.00, 180.00",
            "-90.01, 180.01",
            "90.01, -180.01"
    })
    void shouldThrowExceptionWhenCoordinatesAreOutOfRange(double latitude, double longitude) {
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(latitude, longitude));
    }

    @Test
    void shouldThrowExceptionWhenValueIsNotFinite() {

        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(12.34, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(Double.NaN, 56.78));
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(12.34, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(Double.POSITIVE_INFINITY, 56.78));

    }
}