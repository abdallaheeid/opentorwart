package org.opentorwart.opentorwart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opentorwart.upstream")
public record UpstreamProperties(
        String baseUrl,
        String apiKey
) {
}
