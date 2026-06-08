-- V001: Security Schema + Seed Data
-- Tables: users, user_roles, audit_logs
-- Seed: default admin user + role

-- ============================================
-- Users table
-- ============================================

CREATE TABLE users (
    id UUID PRIMARY KEY,
    row_version BIGINT NOT NULL DEFAULT 0,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_active ON users(active);

-- User roles junction table (users can have multiple roles)
CREATE TABLE user_roles (
    id UUID PRIMARY KEY,
    id_user UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    UNIQUE(id_user, role)
);

CREATE INDEX idx_user_roles_user ON user_roles(id_user);
CREATE INDEX idx_user_roles_role ON user_roles(role);

-- ============================================
-- Audit logs table
-- ============================================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    id_user UUID REFERENCES users(id),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(id_user);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- ============================================
-- Default admin user seed data
-- NOTE: This default admin is DELETED and replaced by Ansible deployment
--       with configurable credentials to prevent enumeration attacks
-- NOSONAR: Intentional development-only seed data, replaced in production deployment
-- ============================================

INSERT INTO users (id, username, password, full_name, email, active, created_at, updated_at)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'admin',
    '$2a$10$mMan.18CFTqJA/FVpkJr3OgCD0uTuhF9Enjf99QHm9tWPJH.nCj5S', -- NOSONAR secrets:S8215 - Development seed, replaced by Ansible in production
    'Administrator',
    'admin@artivisi.com',
    TRUE,
    NOW(),
    NOW()
);

-- Assign ADMIN role to default admin user
INSERT INTO user_roles (id, id_user, role, created_at, created_by)
VALUES (
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'ADMIN',
    NOW(),
    'system'
);
