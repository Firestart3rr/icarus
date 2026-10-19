package io.github.firestart3rr.icarus.web.dto;

import io.github.firestart3rr.icarus.domain.DistanceUnit;

public record RouteResponse(double distance,
                            DistanceUnit unit) {
}
