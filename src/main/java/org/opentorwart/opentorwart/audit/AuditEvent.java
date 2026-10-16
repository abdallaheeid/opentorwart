package org.opentorwart.opentorwart.audit;

import org.opentorwart.opentorwart.config.GatewayRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID requestId, Instant createdAt, String team, String keyId,
        String model, String upstream, boolean stream, int status, String errorType,
        Long latencyMs, Integer promptTokens, Integer completionTokens, BigDecimal costEur) {

    public static AuditEvent of(GatewayRequest gr, int status, String errorType,
                                Integer promptTokens, Integer completionTokens,
                                BigDecimal costEur) {
        long latencyMs = (System.nanoTime() - gr.startNanos()) / 1_000_000;
        return new AuditEvent(gr.requestId(), gr.receivedAt(), gr.team(), gr.keyId(),
                gr.model(), null, gr.stream(), status, errorType,
                latencyMs, promptTokens, completionTokens, costEur);
    }

    /** Shortcut for failures: no tokens, no cost. */
    public static AuditEvent failure(GatewayRequest gr, int status, String errorType) {
        return of(gr, status, errorType, null, null, null);
    }
}

