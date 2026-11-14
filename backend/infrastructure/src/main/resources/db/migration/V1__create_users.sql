CREATE TABLE users (
                       id                 UUID PRIMARY KEY,
                       full_name          VARCHAR(120) NOT NULL,
                       phone              VARCHAR(20)  NOT NULL,
                       email              VARCHAR(160),
                       password_hash      VARCHAR(100) NOT NULL,
                       municipality       VARCHAR(80)  NOT NULL,
                       district           VARCHAR(80),
                       policy_version     VARCHAR(20)  NOT NULL,
                       policy_accepted_at TIMESTAMPTZ  NOT NULL,
                       created_at         TIMESTAMPTZ  NOT NULL,
                       CONSTRAINT uq_users_phone UNIQUE (phone),
                       CONSTRAINT uq_users_email UNIQUE (email)
);