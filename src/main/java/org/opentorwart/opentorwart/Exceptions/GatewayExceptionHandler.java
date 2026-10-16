package org.opentorwart.opentorwart.Exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.opentorwart.opentorwart.audit.AuditEvent;
import org.opentorwart.opentorwart.audit.AuditService;
import org.opentorwart.opentorwart.budget.BudgetService;
import org.opentorwart.opentorwart.config.GatewayRequest;
import org.opentorwart.opentorwart.logging.RequestLog;
import org.opentorwart.opentorwart.logging.RequestLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class GatewayExceptionHandler {

    private final AuditService auditService;
    private final BudgetService budgetService;


    public GatewayExceptionHandler(AuditService auditService, BudgetService budgetService, RequestLogService requestLogService) {
        this.auditService = auditService;
        this.budgetService = budgetService;
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<?> handleUpstreamError(RestClientResponseException ex,
                                                      HttpServletRequest request) {
        handleFailure(request, ex.getStatusCode().value(), "upstream_error");
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ex.getResponseBodyAsString());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<?> handleUpstreamUnreachable(ResourceAccessException ex,
                                                            HttpServletRequest request) {
        handleFailure(request, HttpStatus.BAD_GATEWAY.value(), "upstream_unreachable");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                      {"error":{"message":"Upstream unreachable","type":"upstream_error"}}
                      """);
    }

    private void handleFailure(HttpServletRequest request, int status, String errorType) {
        if (request.getAttribute(GatewayRequest.ATTRIBUTE) instanceof GatewayRequest gr) {
            if (gr.reservation() != null) {
                budgetService.settle(gr.reservation(), 0);
            }
            auditService.record(AuditEvent.failure(gr, status, errorType));
        }
    }

    @ExceptionHandler(BudgetExceededException.class)
    public ResponseEntity<String> handleBudgetExceeded(BudgetExceededException ex,
                                                       HttpServletRequest request) {
        handleFailure(request, HttpStatus.TOO_MANY_REQUESTS.value(), "insufficient_quota");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                  {"error":{"message":"Monthly token budget exceeded","type":"insufficient_quota","code":"insufficient_quota"}}
                  """);
    }

}
