package io.github.firestart3rr.icarus.web;

import io.github.firestart3rr.icarus.distance.HaversineDistanceCalculator;
import io.github.firestart3rr.icarus.domain.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RouteController.class)
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HaversineDistanceCalculator distanceCalculator;

    @Test
    @DisplayName("Should return 200 with distance in meters and map request to domain points")
    void shouldCalculateDistance() throws Exception {

        String requestJson = """
                {
                  "start": {
                    "latitude": 52.2297,
                    "longitude": 21.0122
                  },
                  "end": {
                    "latitude": 50.0647,
                    "longitude": 19.9450
                  }
                }
                """;

        GeoPoint start = new GeoPoint(52.2297, 21.0122);
        GeoPoint end = new GeoPoint(50.0647, 19.9450);
        double stubbedDistance = 123456.789;

        when(distanceCalculator.calculateDistance(any(GeoPoint.class), any(GeoPoint.class)))
                .thenReturn(stubbedDistance);

        mockMvc.perform(post("/api/v1/routes/distance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.distance").value(stubbedDistance))
                .andExpect(jsonPath("$.unit").value("METERS"));

        verify(distanceCalculator).calculateDistance(start, end);
        verifyNoMoreInteractions(distanceCalculator);
    }

    @Test
    @DisplayName("Should return 400 with start.latitude as invalid field")
    void shouldReturn400WhenStartLatitudeOutOfRange() throws Exception {

        String requestJson = """
                {
                  "start": {
                    "latitude": 500,
                    "longitude": 21.0122
                  },
                  "end": {
                    "latitude": 50.0647,
                    "longitude": 19.9450
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/routes/distance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.length()").value(1))
                .andExpect(jsonPath("$.invalidParams[0].field").value("start.latitude"))
                .andExpect(jsonPath("$.invalidParams[0].reason").value("Latitude must be between -90 and 90"));

        verifyNoInteractions(distanceCalculator);
    }

    @Test
    @DisplayName("Should return 400 when end point is missing")
    void shouldReturn400WhenEndPointIsMissing() throws Exception {
        String requestJson = """
                {
                  "start": {
                    "latitude": 52.2297,
                    "longitude": 21.0122
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/routes/distance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.length()").value(1))
                .andExpect(jsonPath("$.invalidParams[0].field").value("end"))
                .andExpect(jsonPath("$.invalidParams[0].reason").value("end point cannot be null"));

        verifyNoInteractions(distanceCalculator);
    }

    @Test
    @DisplayName("Should return 400 and violations when multiple fields are invalid")
    void shouldReturnAllViolationsWhenMultipleFieldsInvalid() throws Exception {
        String requestJson = """
                {
                  "start": {
                    "latitude": 500,
                    "longitude": 21.0122
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/routes/distance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.length()").value(2))
                .andExpect(jsonPath("$.invalidParams[*].field", containsInAnyOrder("start.latitude", "end")));

        verifyNoInteractions(distanceCalculator);
    }

    @Test
    @DisplayName("Should reject NaN latitude in validation with start.latitude as invalid field")
    void shouldRejectNaNLatitudeInValidation() throws Exception {
        String requestJson = """
                {
                  "start": {
                    "latitude": "NaN",
                    "longitude": 21.0122
                  },
                  "end": {
                    "latitude": 50.0647,
                    "longitude": 19.9450
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/routes/distance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.length()").value(1))
                .andExpect(jsonPath("$.invalidParams[0].field").value("start.latitude"))
                .andExpect(jsonPath("$.invalidParams[0].reason").value("Latitude must be between -90 and 90"));

        verifyNoInteractions(distanceCalculator);
    }

    @Test
    @DisplayName("Should return 400 without invalidParams when request body cannot be parsed")
    void shouldReturn400WhenCantReadRequest() throws Exception {
        String requestJson = """
                {
                  "start": {
                    "latitude": "abc",
                    "longitude": 21.0122
                  },
                  "end": {
                    "latitude": 50.0647,
                    "longitude": 19.9450
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/routes/distance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.detail", not(containsStringIgnoringCase("Double"))))
                .andExpect(jsonPath("$.detail", not(containsStringIgnoringCase("Jackson"))))
                .andExpect(jsonPath("$.detail", not(containsStringIgnoringCase("Java"))))
                .andExpect(jsonPath("$.invalidParams").doesNotExist());

        verifyNoInteractions(distanceCalculator);
    }

    @Test
    @DisplayName("Should return 500 when service throws Illegal Argument Exception")
    void shouldReturn500WithoutExceptionMessageWhenServiceThrowsIllegalArgumentException() throws Exception {
        String requestJson = """
                {
                  "start": {
                    "latitude": 52.2297,
                    "longitude": 21.0122
                  },
                  "end": {
                    "latitude": 50.0647,
                    "longitude": 19.9450
                  }
                }
                """;

        String internalSecretMessage = "Geodetic library internal error: singular matrix";
        when(distanceCalculator.calculateDistance(any(GeoPoint.class), any(GeoPoint.class)))
                .thenThrow(new IllegalArgumentException(internalSecretMessage));

        mockMvc.perform(post("/api/v1/routes/distance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.detail", not(containsStringIgnoringCase(internalSecretMessage))))
                .andExpect(jsonPath("$.detail", not(containsStringIgnoringCase("IllegalArgumentException"))))
                .andExpect(content().string(not(containsString(internalSecretMessage))))
                .andExpect(jsonPath("$.incidentId").exists());
    }

}