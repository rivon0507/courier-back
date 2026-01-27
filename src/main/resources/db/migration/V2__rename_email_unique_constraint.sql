-- noinspection SqlResolveForFile @ constraint/"users_email_key"

ALTER TABLE users
    DROP CONSTRAINT users_email_key;

ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email);
