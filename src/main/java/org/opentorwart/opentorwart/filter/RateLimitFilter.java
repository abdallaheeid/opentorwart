package org.opentorwart.opentorwart.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.opentorwart.opentorwart.config.RateLimitProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitProperties props;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String keyId = (String) request.getAttribute(ApiKeyAuthFilter.KEY_ATTRIBUTE);
        if (keyId == null) {            // defensive: auth should have rejected already
            chain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(keyId, k -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = (probe.getNanosToWaitForRefill() + 999_999_999) / 1_000_000_000;
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"error":{"message":"Rate limit exceeded","type":"rate_limit_exceeded"}}
                """);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(props.capacity())
                        .refillGreedy(props.refillPerMinute(), Duration.ofMinutes(1)))
                .build();
    }
}
