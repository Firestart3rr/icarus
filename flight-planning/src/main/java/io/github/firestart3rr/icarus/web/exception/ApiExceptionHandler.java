package io.github.firestart3rr.icarus.web.exception;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.UUID;


@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI VALIDATION_ERROR_TYPE = URI.create("https://firestart3rr.github.io/icarus/problems/validation-error");
    private static final URI INTERNAL_SERVER_ERROR_TYPE = URI.create("https://firestart3rr.github.io/icarus/problems/internal-server-error");

    public record InvalidParam(String field, String reason) {
    }

    // Handling DTO validation errors—multiple errors at once
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {

        List<InvalidParam> invalidParams = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new InvalidParam(error.getField(), error.getDefaultMessage()))
                .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status,
                "Requested data are incomplete or exceed allowed range. Check 'invalidParams' for details.");
        problemDetail.setType(VALIDATION_ERROR_TYPE);
        problemDetail.setTitle("Request validation error");
        problemDetail.setProperty("invalidParams", invalidParams);

        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUncaughtException(Exception ex, WebRequest request) {

        String incidentId = UUID.randomUUID().toString();

        logger.error("Unhandled application error [incidentId: {" + incidentId + "}]");

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status,
                "Internal server error occured, please contact with administrator."
        );
        problemDetail.setType(INTERNAL_SERVER_ERROR_TYPE);
        problemDetail.setTitle("Internal server error");
        problemDetail.setProperty("incidentId", incidentId);

        return handleExceptionInternal(ex, problemDetail, new HttpHeaders(), status, request);
    }

}
