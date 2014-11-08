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
  full_name          VARCHAR(255),
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  modified_at TIMESTAMP AS NOW()
);

DROP TABLE IF EXISTS user_roles;
CREATE TABLE user_roles (
  user_id     INTEGER                             NOT NULL REFERENCES users (id),
  role        VARCHAR(20)                         NOT NULL REFERENCES roles (name),
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  modified_at TIMESTAMP AS NOW()
);

DROP INDEX IF EXISTS ix_user_roles;
CREATE UNIQUE INDEX ix_user_roles ON user_roles (user_id, role);

//--- Customers --------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS customers;
CREATE TABLE customers (
  id          INTEGER                             NOT NULL IDENTITY,
  name    VARCHAR(100) NOT NULL,
  name_lc VARCHAR(100) AS LOWER(name),
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  modified_at TIMESTAMP AS NOW()
);

DROP INDEX IF EXISTS ix_customer_name_lc;
CREATE UNIQUE INDEX ix_customer_name_lc ON customers (name_lc);

DROP TABLE IF EXISTS customer_members;
CREATE TABLE customer_members (
  customer_id     INTEGER                             NOT NULL REFERENCES customers (id),
  user_id         INTEGER                             NOT NULL REFERENCES users (id),
  primary_contact BOOLEAN DEFAULT FALSE               NOT NULL,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  modified_at TIMESTAMP AS NOW()
);

DROP INDEX IF EXISTS ix_customer_members;
CREATE UNIQUE INDEX ix_customer_members ON customer_members (customer_id, user_id);

//--- Applications ---------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS applications;
CREATE TABLE applications (
  id          INTEGER                             NOT NULL IDENTITY,
  customer_id INTEGER                             NOT NULL REFERENCES customers (id),
  name        VARCHAR(100)                        NOT NULL,
  version     VARCHAR(100),
  environment VARCHAR(100),
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  modified_at TIMESTAMP AS NOW()
);

DROP INDEX IF EXISTS ix_applications;
CREATE UNIQUE INDEX ix_applications ON applications (customer_id, name);

//--- JVM runs -------------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS jvm_runs;
CREATE TABLE jvm_runs (
  id                INTEGER      NOT NULL IDENTITY,
  customer_id       INTEGER      NOT NULL REFERENCES customers (id),
  application_id    INTEGER      NOT NULL REFERENCES applications (id),
  host_name         VARCHAR(255) NOT NULL,
  jvm_fingerprint   VARCHAR(50)  NOT NULL,
  codekvast_version VARCHAR(20)  NOT NULL,
  codekvast_vcs_id  VARCHAR(50)  NOT NULL,
  started_at        TIMESTAMP    NOT NULL,
  dumped_at         TIMESTAMP    NOT NULL
);

DROP INDEX IF EXISTS ix_jvm_runs;
CREATE UNIQUE INDEX ix_jvm_runs ON jvm_runs (customer_id, application_id, jvm_fingerprint);

//--- Signatures -----------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS signatures;
CREATE TABLE signatures (
  id              INTEGER       NOT NULL IDENTITY,
  customer_id     INTEGER       NOT NULL REFERENCES customers (id),
  application_id  INTEGER       NOT NULL REFERENCES applications (id),
  signature VARCHAR(2000) NOT NULL,
  jvm_fingerprint VARCHAR(50),
  used_at         TIMESTAMP,
  confidence      TINYINT
);

DROP INDEX IF EXISTS ix_signatures;
CREATE UNIQUE INDEX ix_signatures ON signatures (customer_id, application_id, signature);

//--- System data ----------------------------------------------------------------------------------------------
INSERT INTO roles (name) VALUES ('SUPERUSER');
INSERT INTO roles (name) VALUES ('AGENT');
INSERT INTO roles (name) VALUES ('ADMIN');
INSERT INTO roles (name) VALUES ('USER');
INSERT INTO roles (name) VALUES ('MONITOR');

// --- System account ---------------------------------------------------------------------------
INSERT INTO customers (id, name) VALUES (0, 'System');

INSERT INTO users (id, username, plaintext_password, enabled) VALUES (0, 'system', '0000', FALSE);
INSERT INTO customer_members (customer_id, user_id) VALUES (0, 0);
INSERT INTO user_roles (user_id, role) VALUES (0, 'SUPERUSER');
INSERT INTO user_roles (user_id, role) VALUES (0, 'AGENT');
INSERT INTO user_roles (user_id, role) VALUES (0, 'ADMIN');
INSERT INTO user_roles (user_id, role) VALUES (0, 'USER');
INSERT INTO user_roles (user_id, role) VALUES (0, 'MONITOR');

INSERT INTO users (id, username, plaintext_password, enabled) VALUES (1, 'monitor', '0000', TRUE);
INSERT INTO customer_members (customer_id, user_id) VALUES (0, 1);
INSERT INTO user_roles (user_id, role) VALUES (1, 'MONITOR');

// --- Demo account ---------------------------------------------------------------------------
INSERT INTO customers (id, name) VALUES (1, 'Demo');

INSERT INTO users (id, username, plaintext_password, enabled) VALUES (2, 'agent', '0000', TRUE);
INSERT INTO customer_members (customer_id, user_id) VALUES (1, 2);
INSERT INTO user_roles (user_id, role) VALUES (2, 'AGENT');

INSERT INTO users (id, username, plaintext_password, enabled) VALUES (3, 'admin', '0000', TRUE);
INSERT INTO customer_members (customer_id, user_id) VALUES (1, 3);
INSERT INTO user_roles (user_id, role) VALUES (3, 'ADMIN');
INSERT INTO user_roles (user_id, role) VALUES (3, 'USER');

INSERT INTO users (id, username, plaintext_password, enabled) VALUES (4, 'user', '0000', TRUE);
INSERT INTO customer_members (customer_id, user_id) VALUES (1, 4);
INSERT INTO user_roles (user_id, role) VALUES (4, 'USER');
