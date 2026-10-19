package io.github.firestart3rr.icarus.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RouteRequest(@Valid @NotNull(message = "start point cannot be null") PointDto start,
                           @Valid @NotNull(message = "end point cannot be null") PointDto end) {
}
