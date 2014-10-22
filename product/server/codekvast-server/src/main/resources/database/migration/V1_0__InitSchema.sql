//--- Roles ----------------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
  name VARCHAR(20) NOT NULL UNIQUE,
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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS user_roles;
CREATE TABLE user_roles (
  user_id    INTEGER                             NOT NULL REFERENCES users (id),
  role       VARCHAR(20)                         NOT NULL REFERENCES roles (name),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP INDEX IF EXISTS ix_user_roles;
CREATE UNIQUE INDEX ix_user_roles ON user_roles (user_id, role);

//--- Customers --------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS customers;
CREATE TABLE customers (
  id         INTEGER                             NOT NULL IDENTITY,
  name       VARCHAR(100)                        NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS customer_members;
CREATE TABLE customer_members (
  customer_id     INTEGER                             NOT NULL REFERENCES customers (id),
  user_id         INTEGER                             NOT NULL REFERENCES users (id),
  primary_contact BOOLEAN DEFAULT FALSE               NOT NULL,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP INDEX IF EXISTS ix_customer_members;
CREATE UNIQUE INDEX ix_customer_members ON customer_members (customer_id, user_id);

//--- Applications ---------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS applications;
CREATE TABLE applications (
  id          INTEGER                             NOT NULL IDENTITY,
  customer_id INTEGER                             NOT NULL REFERENCES customers (id),
  name        VARCHAR(100)                        NOT NULL,
  version     VARCHAR(100)                        NOT NULL,
  environment VARCHAR(100)                        NOT NULL,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP INDEX IF EXISTS ix_applications;
CREATE UNIQUE INDEX ix_applications ON applications (customer_id, name);

//--- System data ----------------------------------------------------------------------------------------------
INSERT INTO roles (name) VALUES ('SUPERUSER');
INSERT INTO roles (name) VALUES ('AGENT');
INSERT INTO roles (name) VALUES ('USER');
INSERT INTO roles (name) VALUES ('MONITOR');
// The plaintext passwords will be hashed when the application starts
INSERT INTO users (id, username, plaintext_password, enabled) VALUES (0, 'system', '0000', FALSE);
INSERT INTO users (id, username, plaintext_password, enabled) VALUES (1, 'agent', '0000', TRUE);
INSERT INTO users (id, username, plaintext_password, enabled) VALUES (2, 'user', '0000', TRUE);
INSERT INTO users (id, username, plaintext_password, enabled) VALUES (3, 'monitor', '0000', TRUE);
INSERT INTO user_roles (user_id, role) VALUES (0, 'SUPERUSER');
INSERT INTO user_roles (user_id, role) VALUES (0, 'AGENT');
INSERT INTO user_roles (user_id, role) VALUES (0, 'USER');
INSERT INTO user_roles (user_id, role) VALUES (0, 'MONITOR');
INSERT INTO user_roles (user_id, role) VALUES (1, 'AGENT');
INSERT INTO user_roles (user_id, role) VALUES (2, 'USER');
INSERT INTO user_roles (user_id, role) VALUES (3, 'MONITOR');
