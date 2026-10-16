package org.opentorwart.opentorwart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

@ConfigurationProperties(prefix = "opentorwart.pricing")
public record PricingProperties(Map<String, ModelPrice> models) {
    public record ModelPrice(BigDecimal input, BigDecimal output) {}
}
