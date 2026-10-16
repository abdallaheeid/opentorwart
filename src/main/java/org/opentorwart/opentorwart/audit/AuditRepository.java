package org.opentorwart.opentorwart.audit;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
public class AuditRepository {
    private final JdbcClient client;

    public AuditRepository(JdbcClient client) {
        this.client = client;
    }

    public void insert(AuditEvent event) {
        String sql = """
            INSERT INTO audit_log (
                request_id, created_at, team, key_id, model, upstream, stream, status,
                error_type, latency_ms, prompt_tokens, completion_tokens, cost_eur)
            VALUES (
                :request_id, :created_at, :team, :key_id, :model, :upstream, :stream, :status,
                :error_type, :latency_ms, :prompt_tokens, :completion_tokens, :cost_eur)
            """;

        client.sql(sql)
                .param("request_id", event.requestId())
                .param("created_at", OffsetDateTime.ofInstant(event.createdAt(), ZoneOffset.UTC))
                .param("team", event.team())
                .param("key_id", event.keyId())
                .param("model", event.model())
                .param("upstream", event.upstream())
                .param("stream", event.stream())
                .param("status", event.status())
                .param("error_type", event.errorType())
                .param("latency_ms", event.latencyMs())
                .param("prompt_tokens", event.promptTokens())
                .param("completion_tokens", event.completionTokens())
                .param("cost_eur", event.costEur())
                .update();
    }
}
