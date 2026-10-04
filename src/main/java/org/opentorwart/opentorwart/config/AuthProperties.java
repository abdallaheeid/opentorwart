package org.opentorwart.opentorwart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "opentorwart.auth")
public record AuthProperties(List<ApiKey> keys) {
    public record ApiKey(String team, String keyHash) {}
}
