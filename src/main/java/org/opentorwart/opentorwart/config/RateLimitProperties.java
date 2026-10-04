package org.opentorwart.opentorwart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opentorwart.rate-limit")
public record RateLimitProperties(int capacity,
                                  int refillPerMinute) { }
