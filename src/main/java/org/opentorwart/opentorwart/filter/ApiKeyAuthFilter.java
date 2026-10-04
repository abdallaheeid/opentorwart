package org.opentorwart.opentorwart.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.opentorwart.opentorwart.config.AuthProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String TEAM_ATTRIBUTE = "opentorwart.team";
    public static final String KEY_ATTRIBUTE = "opentorwart.key";

    private final Map<String, String> teamByHash;

    public ApiKeyAuthFilter(AuthProperties props) {
        this.teamByHash = props.keys().stream()
                .collect(Collectors.toMap(k -> k.keyHash().toLowerCase(), AuthProperties.ApiKey::team));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(response, "Missing API key");
            return;
        }

        String keyHash = sha256(header.substring(7).trim());
        String team = teamByHash.get(keyHash);
        if (team == null) {
            unauthorized(response, "Invalid API key");
            return;
        }

        request.setAttribute(TEAM_ATTRIBUTE, team);
        request.setAttribute(KEY_ATTRIBUTE, keyHash);
        chain.doFilter(request, response);       // continue to the controller
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"error":{"message":"%s","type":"invalid_api_key"}}
                """.formatted(message));
    }
}
