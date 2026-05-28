CREATE TABLE org_department (
    dept_id BIGINT PRIMARY KEY,
    parent_dept_id BIGINT,
    name VARCHAR(255) NOT NULL,
    dept_order BIGINT,
    deleted_at TIMESTAMPTZ,
    last_sync_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_org_department_parent_dept ON org_department(parent_dept_id);

CREATE TABLE org_user_profile (
    user_id VARCHAR(128) PRIMARY KEY,
    union_id VARCHAR(128),
    name VARCHAR(128) NOT NULL,
    title VARCHAR(128),
    mobile VARCHAR(64),
    email VARCHAR(256),
    active BOOLEAN NOT NULL DEFAULT true,
    manager_user_id VARCHAR(128),
    primary_dept_id BIGINT,
    last_sync_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_org_user_profile_union_id ON org_user_profile(union_id);
CREATE INDEX idx_org_user_profile_primary_dept ON org_user_profile(primary_dept_id);

CREATE TABLE org_user_department (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    dept_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_org_user_department UNIQUE (user_id, dept_id)
);

CREATE INDEX idx_org_user_department_dept ON org_user_department(dept_id);

CREATE TABLE org_role (
    role_id BIGINT PRIMARY KEY,
    group_id BIGINT,
    group_name VARCHAR(128),
    name VARCHAR(128) NOT NULL,
    last_sync_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE org_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    role_id BIGINT NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    source_scope VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_org_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_org_user_role_user ON org_user_role(user_id);

CREATE TABLE org_sync_state (
    sync_key VARCHAR(64) PRIMARY KEY,
    phase VARCHAR(64) NOT NULL,
    cursor_value VARCHAR(256),
    success BOOLEAN NOT NULL DEFAULT false,
    last_error TEXT,
    last_run_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE org_event_checkpoint (
    event_key VARCHAR(256) PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    error_message TEXT
);

ALTER TABLE user_account
    ADD COLUMN IF NOT EXISTS permission_source VARCHAR(64),
    ADD COLUMN IF NOT EXISTS permission_initialized_at TIMESTAMPTZ;
