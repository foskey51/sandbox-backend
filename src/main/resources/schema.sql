-- DROP tables if exist (for dev/testing)

DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       full_name TEXT NULL,
                       bio TEXT NULL,
                       username TEXT NULL,
                       email TEXT NOT NULL UNIQUE,
                       password TEXT NOT NULL,
                       roles TEXT[] NOT NULL,
                       profile_image BYTEA NULL
);

-- Create refresh_tokens table
CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token TEXT NOT NULL,
                                expires_at BIGINT NOT NULL
);
