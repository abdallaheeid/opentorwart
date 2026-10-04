package org.opentorwart.opentorwart.logging;

public record RequestLog (
        String team,
        String model,
        boolean stream,
        int status,
        long upstreamLatencyMs,
        Integer promptTokens,
        Integer completionTokens
) {
}
