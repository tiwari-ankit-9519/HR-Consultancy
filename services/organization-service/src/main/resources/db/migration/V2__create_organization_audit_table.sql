CREATE TABLE organization_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    organization_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(255),
    request_id VARCHAR(64),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_organization_audit_action CHECK (
        action IN (
            'CREATE',
            'UPDATE',
            'DELETE',
            'DEACTIVATE'
        )
    )
);

CREATE INDEX idx_organization_audit_org_id ON organization_audit (organization_id);

CREATE INDEX idx_organization_audit_changed_at ON organization_audit (changed_at);

CREATE INDEX idx_organization_audit_action ON organization_audit (action);