package org.opentorwart.opentorwart.budget;

import org.opentorwart.opentorwart.config.BudgetProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class TokenEstimator {

    private final BudgetProperties props;

    public TokenEstimator(BudgetProperties props) {
        this.props = props;
    }

    /** Estimated prompt tokens + max output tokens. */
    public long estimate(JsonNode request) {
        long chars = 0;
        for (JsonNode message : request.path("messages")) {
            chars += contentLength(message.path("content"));
        }
        long promptTokens = (chars + 3) / 4;          // ~4 chars per token, rounded up

        JsonNode maxTokens = request.has("max_completion_tokens")
                ? request.get("max_completion_tokens")
                : request.path("max_tokens");
        long outputTokens = maxTokens.isNumber()
                ? maxTokens.asLong()
                : props.defaultMaxOutputTokens();

        return promptTokens + outputTokens;
    }

    private long contentLength(JsonNode content) {
        if (content.isString()) {                     // "content": "Hi"
            return content.asString().length();
        }
        long total = 0;
        for (JsonNode part : content) {               // "content": [{"type":"text","text":"Hi"}, ...]
            total += part.path("text").asString("").length();
        }
        return total;
    }
}
