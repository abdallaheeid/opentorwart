CREATE TABLE audit_log (
       request_id        UUID PRIMARY KEY,
       created_at        TIMESTAMPTZ   NOT NULL,
       team              TEXT          NOT NULL,
       key_id            TEXT          NOT NULL,
       model             TEXT,
       upstream          TEXT,
       stream            BOOLEAN       NOT NULL,
       status            INT           NOT NULL,
       error_type        TEXT,
       latency_ms        INT,
       prompt_tokens     INT,
       completion_tokens INT,
       cost_eur          NUMERIC(12, 6)
);

CREATE INDEX idx_audit_log_team_created ON audit_log (team, created_at);