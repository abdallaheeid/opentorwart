package org.opentorwart.opentorwart.config;

import java.time.Instant;
import java.util.UUID;

public record GatewayRequest(UUID requestId, Instant receivedAt, String team, String keyId, String model, boolean stream,
                             long startNanos, Reservation reservation) {

    public static final String ATTRIBUTE = "opentorwart.request";

    public GatewayRequest withReservation(Reservation reservation) {
        return new GatewayRequest(requestId, receivedAt, team, keyId, model, stream, startNanos, reservation);
    }
}
