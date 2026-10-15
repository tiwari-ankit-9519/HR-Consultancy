-- ============================================================
-- V2: User profiles, departments, and document metadata
-- Schema: user_management
-- ============================================================

-- 1. Employee code sequence
CREATE SEQUENCE user_management.employee_code_seq START
WITH
    1 INCREMENT BY 1 NO CYCLE;

-- 2. User profile
-- Keycloak remains authoritative for authentication,
-- passwords, and roles.
CREATE TABLE user_management.user_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    keycloak_user_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone_number VARCHAR(30),
    job_title VARCHAR(100),
    organization_id UUID,
    employee_code VARCHAR(20) NOT NULL UNIQUE DEFAULT(
        'EMP-' || to_char (
            nextval (
                'user_management.employee_code_seq'
            ),
            'FM000000'
        )
    ),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    onboarding_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMPTZ,
    CONSTRAINT chk_user_profile_status CHECK (
        status IN ('ACTIVE', 'INACTIVE')
    ),
    CONSTRAINT chk_user_profile_deactivation CHECK (
        (
            status = 'ACTIVE'
            AND deactivated_at IS NULL
        )
        OR (
            status = 'INACTIVE'
            AND deactivated_at IS NOT NULL
        )
    ),
    CONSTRAINT chk_user_profile_onboarding CHECK (
        onboarding_status IN (
            'PENDING',
            'INVITATION_SENT',
            'COMPLETED',
            'INVITATION_FAILED'
        )
    )
);

CREATE INDEX idx_user_profile_organization_id ON user_management.user_profile (organization_id);

CREATE INDEX idx_user_profile_status ON user_management.user_profile (status);

CREATE INDEX idx_user_profile_onboarding_status ON user_management.user_profile (onboarding_status);

-- 3. Department master
-- Departments are scoped to an organization.
-- organization_id references the organization service;
-- no cross-service foreign key is created.
CREATE TABLE user_management.department (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    organization_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_department_org_code UNIQUE (organization_id, code),
    CONSTRAINT uq_department_org_name UNIQUE (organization_id, name),
    CONSTRAINT chk_department_status CHECK (
        status IN ('ACTIVE', 'INACTIVE')
    )
);

CREATE INDEX idx_department_organization_id ON user_management.department (organization_id);

CREATE INDEX idx_department_status ON user_management.department (status);

-- 4. User-to-department assignments
-- Supports multiple departments per user.
CREATE TABLE user_management.user_department (
    user_id UUID NOT NULL,
    department_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by_keycloak_user_id VARCHAR(255),
    CONSTRAINT pk_user_department PRIMARY KEY (user_id, department_id),
    CONSTRAINT fk_user_department_user FOREIGN KEY (user_id) REFERENCES user_management.user_profile (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_department_department FOREIGN KEY (department_id) REFERENCES user_management.department (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_department_department_id ON user_management.user_department (department_id);

-- 5. User document metadata
-- Actual file contents remain in private S3-compatible storage.
CREATE TABLE user_management.user_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL,
    document_group_id UUID,
    document_type VARCHAR(30) NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    storage_key VARCHAR(1024) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64),
    verification_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    uploaded_by_keycloak_user_id VARCHAR(255) NOT NULL,
    verified_by_keycloak_user_id VARCHAR(255),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_user_document_profile FOREIGN KEY (user_id) REFERENCES user_management.user_profile (id) ON DELETE RESTRICT,
    CONSTRAINT chk_user_document_type CHECK (
        document_type IN (
            'PROFILE_IMAGE',
            'GOVERNMENT_ID_FRONT',
            'GOVERNMENT_ID_BACK',
            'OTHER_ID_DOCUMENT'
        )
    ),
    CONSTRAINT chk_user_document_size CHECK (file_size_bytes > 0),
    CONSTRAINT chk_user_document_verification CHECK (
        verification_status IN (
            'NOT_REQUIRED',
            'PENDING',
            'VERIFIED',
            'REJECTED'
        )
    ),
    CONSTRAINT chk_user_document_verification_timestamp CHECK (
        (
            verification_status = 'VERIFIED'
            AND verified_at IS NOT NULL
        )
        OR (
            verification_status <> 'VERIFIED'
        )
    )
);

CREATE INDEX idx_user_document_user_id ON user_management.user_document (user_id);

CREATE INDEX idx_user_document_type ON user_management.user_document (document_type);

CREATE INDEX idx_user_document_verification ON user_management.user_document (verification_status);

-- At most one current, non-deleted profile image per user.
CREATE UNIQUE INDEX uq_user_current_profile_image ON user_management.user_document (user_id)
WHERE
    document_type = 'PROFILE_IMAGE'
    AND is_current = TRUE
    AND deleted_at IS NULL;