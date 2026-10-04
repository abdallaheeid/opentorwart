package org.opentorwart.opentorwart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class UpstreamClientConfig {

    @Bean
    RestClient upstreamClient(RestClient.Builder builder, UpstreamProperties props) {
        builder.baseUrl(props.baseUrl());
        if (StringUtils.hasText(props.apiKey()) ) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey());
        }
        return builder.build();
    }

}
