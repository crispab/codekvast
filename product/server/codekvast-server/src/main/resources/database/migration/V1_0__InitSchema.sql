//--- Roles ----------------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
  name VARCHAR(20) NOT NULL UNIQUE,
);

//--- Organisations --------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS organisations;
CREATE TABLE organisations (
  id         INTEGER                             NOT NULL IDENTITY,
  name       VARCHAR(100)                        NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

//--- Users ----------------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS users;
CREATE TABLE users (
  id                 INTEGER                             NOT NULL IDENTITY,
  username           VARCHAR(100)                        NOT NULL UNIQUE,
  encoded_password   VARCHAR(80),
  plaintext_password VARCHAR(255),
  enabled            BOOLEAN DEFAULT TRUE                NOT NULL,
  email              VARCHAR(255),
  name               VARCHAR(255),
  created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS organisation_members;
CREATE TABLE organisation_members (
  organisation_id INTEGER                             NOT NULL REFERENCES organisations (id),
  user_id         INTEGER                             NOT NULL REFERENCES users (id),
  primary_contact BOOLEAN DEFAULT FALSE               NOT NULL,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP INDEX IF EXISTS ix_organisation_members;
CREATE UNIQUE INDEX ix_organisation_members ON organisation_members (organisation_id, user_id);

DROP TABLE IF EXISTS user_roles;
CREATE TABLE user_roles (
  user_id    INTEGER                             NOT NULL REFERENCES users (id),
  role       VARCHAR(20)                         NOT NULL REFERENCES roles (name),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP INDEX IF EXISTS ix_user_roles;
CREATE UNIQUE INDEX ix_user_roles ON user_roles (user_id, role);

//--- System data ----------------------------------------------------------------------------------------------
INSERT INTO roles (name) VALUES ('SUPERUSER');
INSERT INTO roles (name) VALUES ('AGENT');
INSERT INTO roles (name) VALUES ('USER');
INSERT INTO roles (name) VALUES ('MONITOR');
INSERT INTO organisations (id, name) VALUES (0, 'CodeKvast');
INSERT INTO organisations (id, name) VALUES (1, 'Demo');
// The plaintext passwords will be hashed when the application starts
INSERT INTO users (id, username, plaintext_password) VALUES (0, 'system', '0000');
INSERT INTO users (id, username, plaintext_password) VALUES (1, 'agent', '0000');
INSERT INTO users (id, username, plaintext_password) VALUES (2, 'user', '0000');
INSERT INTO users (id, username, plaintext_password) VALUES (3, 'monitor', '0000');
INSERT INTO organisation_members (organisation_id, user_id, primary_contact) VALUES (0, 0, TRUE);
INSERT INTO organisation_members (organisation_id, user_id, primary_contact) VALUES (0, 1, FALSE);
INSERT INTO organisation_members (organisation_id, user_id, primary_contact) VALUES (0, 2, FALSE);
INSERT INTO organisation_members (organisation_id, user_id, primary_contact) VALUES (0, 3, FALSE);
INSERT INTO user_roles (user_id, role) VALUES (0, 'SUPERUSER');
INSERT INTO user_roles (user_id, role) VALUES (0, 'AGENT');
INSERT INTO user_roles (user_id, role) VALUES (0, 'USER');
INSERT INTO user_roles (user_id, role) VALUES (0, 'MONITOR');
INSERT INTO user_roles (user_id, role) VALUES (1, 'AGENT');
INSERT INTO user_roles (user_id, role) VALUES (2, 'USER');
INSERT INTO user_roles (user_id, role) VALUES (3, 'MONITOR');
