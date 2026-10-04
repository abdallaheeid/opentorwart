package org.opentorwart.opentorwart.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class UpstreamExceptionHandler {

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleUpstreamError(RestClientResponseException ex) {
        return ResponseEntity.status(ex.getStatusCode()).
        contentType(MediaType.APPLICATION_JSON).body(ex.getResponseBodyAsString());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<String> handleUpstreamUnreachable(ResourceAccessException ex) {
        String body = """
            {"error":{"message":"Upstream unreachable","type":"upstream_error"}}
            """;
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

}
