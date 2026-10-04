package org.opentorwart.opentorwart.config;

public record GatewayRequest(String team, String model, boolean stream,
                             long startNanos, Reservation reservation) {

    public static final String ATTRIBUTE = "opentorwart.request";

    public GatewayRequest withReservation(Reservation reservation) {
        return new GatewayRequest(team, model, stream, startNanos, reservation);
    }
}
