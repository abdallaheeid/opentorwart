package org.opentorwart.opentorwart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "opentorwart.budget")
public record BudgetProperties(
        boolean failOpen,
        int softCapPercent,
        long defaultMaxOutputTokens,
        Map<String, Long> teams) {}