package io.github.firestart3rr.icarus.web.dto;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

public record PointDto (@NotNull @Range(min=-90L,  max=90L, message = "Latitude must be between -90 and 90") Double latitude,
                        @NotNull @Range(min=-180L,  max=180L, message = "Longitude must be between -180 and 180")Double longitude) {
}
