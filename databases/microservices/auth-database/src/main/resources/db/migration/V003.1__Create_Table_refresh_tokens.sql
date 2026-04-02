-- Refresh tokens: stored as SHA-256 hash, never plain text
CREATE TABLE refresh_tokens
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    issued_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME    NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_rt PRIMARY KEY (id),
    CONSTRAINT uq_rt_hash UNIQUE (token_hash),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_rt_user_id ON refresh_tokens (user_id);