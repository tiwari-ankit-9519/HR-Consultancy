CREATE SCHEMA IF NOT EXISTS user_management;


CREATE TABLE user_management.user_audit (
    id UUID PRIMARY KEY,
    target_user_id VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    actor_id VARCHAR(255),
    actor_username VARCHAR(255),
    request_id VARCHAR(64),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_user_audit_action
        CHECK (action IN (
            'CREATE',
            'ROLE_ASSIGNMENT',
            'PROVISIONING_ROLLBACK',
            'PROVISIONING_FAILURE'
        )),

    CONSTRAINT chk_user_audit_outcome
        CHECK (outcome IN (
            'SUCCESS',
            'FAILURE'
        ))
);

CREATE INDEX idx_user_audit_target_time ON user_management.user_audit (
    target_user_id,
    occurred_at DESC
);

CREATE INDEX idx_user_audit_request_id ON user_management.user_audit (request_id);