package io.github.firestart3rr.icarus.web;

import io.github.firestart3rr.icarus.distance.HaversineDistanceCalculator;
import io.github.firestart3rr.icarus.domain.DistanceUnit;
import io.github.firestart3rr.icarus.domain.GeoPoint;
import io.github.firestart3rr.icarus.web.dto.RouteRequest;
import io.github.firestart3rr.icarus.web.dto.RouteResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final HaversineDistanceCalculator distanceCalculator;

    public RouteController(HaversineDistanceCalculator distanceCalculator){
        this.distanceCalculator = distanceCalculator;
    }

    @PostMapping("/distance")
    public ResponseEntity<RouteResponse> calculateDistance(@Valid @RequestBody RouteRequest request) {
        GeoPoint startPoint = new GeoPoint(request.start().latitude(), request.start().longitude());
        GeoPoint endPoint = new GeoPoint(request.end().latitude(), request.end().longitude());

        double distanceInMeters = distanceCalculator.calculateDistance(startPoint, endPoint);

        RouteResponse response = new RouteResponse(distanceInMeters, DistanceUnit.METERS);
        return ResponseEntity.ok(response);
    }
}
