-- Roles ----------------------------------------------------------------------------------------------------
CREATE TABLE roles (
  name VARCHAR(20) NOT NULL UNIQUE,
);
COMMENT ON TABLE roles IS 'Spring Security roles, without the ROLE_ prefix';

-- Users ----------------------------------------------------------------------------------------------------
CREATE TABLE users (
  id                 BIGINT                              NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username           VARCHAR(100)                        NOT NULL UNIQUE,
  encoded_password   VARCHAR(80)                         NULL,
  plaintext_password VARCHAR(255)                        NULL
  COMMENT 'Will be replaced by an encoded password at application startup',
  enabled            BOOLEAN DEFAULT TRUE                NOT NULL,
  email_address      VARCHAR(64)                         NULL UNIQUE,
  full_name          VARCHAR(255)                        NULL,
  created_at         TIMESTAMP DEFAULT current_timestamp NOT NULL,
  modified_at        TIMESTAMP AS NOW()
);

CREATE TABLE user_roles (
  user_id    BIGINT                              NOT NULL REFERENCES users (id),
  role        VARCHAR(20)                         NOT NULL REFERENCES roles (name),
  created_at TIMESTAMP DEFAULT current_timestamp NOT NULL,
  modified_at TIMESTAMP AS NOW()
);

CREATE UNIQUE INDEX ix_user_roles ON user_roles (user_id, role);

-- Organisations --------------------------------------------------------------------------------------------
CREATE TABLE organisations (
  id         BIGINT                              NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100)                        NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT current_timestamp NOT NULL,
  modified_at TIMESTAMP AS NOW()
);

CREATE TABLE organisation_members (
  organisation_id BIGINT NOT NULL REFERENCES organisations (id),
  user_id         BIGINT NOT NULL REFERENCES users (id),
  primary_contact BOOLEAN DEFAULT FALSE               NOT NULL,
  created_at      TIMESTAMP DEFAULT current_timestamp NOT NULL,
  modified_at     TIMESTAMP AS NOW()
);

CREATE UNIQUE INDEX ix_organisation_members ON organisation_members (organisation_id, user_id);

-- Applications ---------------------------------------------------------------------------------------------
CREATE TABLE applications (
  id                       BIGINT                              NOT NULL AUTO_INCREMENT PRIMARY KEY,
  organisation_id          BIGINT        NOT NULL REFERENCES organisations (id),
  name                     VARCHAR(100)  NOT NULL,
  truly_dead_after_seconds INTEGER       NOT NULL
  COMMENT 'After how long can a unused signature in this application be considered dead?',
  notes                    VARCHAR(2000) NULL
  COMMENT 'Free text notes about the application',
  created_at               TIMESTAMP DEFAULT current_timestamp NOT NULL,
  modified_at              TIMESTAMP AS NOW()
);

CREATE UNIQUE INDEX ix_applications ON applications (organisation_id, name);

-- JVM info -------------------------------------------------------------------------------------------------
CREATE TABLE jvm_info (
  id                            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  organisation_id               BIGINT       NOT NULL REFERENCES organisations (id),
  application_id                BIGINT       NOT NULL REFERENCES applications (id),
  application_version           VARCHAR(100) NOT NULL,
  jvm_uuid                      VARCHAR(50)  NOT NULL
  COMMENT 'The UUID generated by each Codekvast Collector instance',
  agent_computer_id             VARCHAR(50)  NOT NULL
  COMMENT 'The se.crisp.codekvast.agent.util.ComputerID value generated by the Codekvast Agent',
  agent_host_name               VARCHAR(255) NOT NULL
  COMMENT 'The hostname of the machine in which Codekvast Agent executes',
  agent_upload_interval_seconds INTEGER      NOT NULL
  COMMENT 'The interval between uploads from this agent (seconds)',
  codekvast_vcs_id              VARCHAR(50)  NOT NULL
  COMMENT 'The Git hash of Codekvast used for collecting the data',
  codekvast_version             VARCHAR(20)  NOT NULL
  COMMENT 'The version of Codekvast used for collecting the data',
  collector_computer_id         VARCHAR(50)  NOT NULL
  COMMENT 'The se.crisp.codekvast.agent.util.ComputerID value generated by the Codekvast Collector',
  collector_host_name           VARCHAR(255) NOT NULL
  COMMENT 'The hostname of the machine in which Codekvast Collector executes',
  collector_resolution_seconds  INTEGER      NOT NULL,
  method_execution_pointcut     VARCHAR(255) NOT NULL,
  started_at_millis             BIGINT       NOT NULL
  COMMENT 'The value of System.currentTimeMillis() when Codekvast Collector instance was started',
  dumped_at_millis              BIGINT       NOT NULL
  COMMENT 'The value of System.currentTimeMillis() when Codekvast Collector made an output of the collected data'
);
COMMENT ON TABLE jvm_info IS 'Data about one JVM that is instrumented by the Codekvast Collector';

CREATE UNIQUE INDEX ix_jvm_info ON jvm_info (organisation_id, application_id, jvm_uuid);

-- Signatures -----------------------------------------------------------------------------------------------
CREATE TABLE signatures (
  id                     BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  organisation_id        BIGINT        NOT NULL REFERENCES organisations (id),
  application_id         BIGINT        NOT NULL REFERENCES applications (id),
  jvm_id                 BIGINT        NOT NULL REFERENCES jvm_info (id),
  signature              VARCHAR(2000) NOT NULL
  COMMENT 'The method signature in human readable format',
  invoked_at_millis      BIGINT        NOT NULL
  COMMENT 'The value of System.currentTimeMillis() the method was invoked (rounded to nearest collection interval). 0 means not yet invoked',
  millis_since_jvm_start BIGINT        NOT NULL
  COMMENT 'The delta between invoked_at_millis and the instant the JVM started',
  confidence             TINYINT       NULL
  COMMENT 'The ordinal for se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence. NULL for not yet invoked.'
);

-- System data ----------------------------------------------------------------------------------------------
INSERT INTO roles (name) VALUES ('SUPERUSER');
INSERT INTO roles (name) VALUES ('AGENT');
INSERT INTO roles (name) VALUES ('ADMIN');
INSERT INTO roles (name) VALUES ('USER');
INSERT INTO roles (name) VALUES ('MONITOR');

-- System account ---------------------------------------------------------------------------
INSERT INTO organisations (id, name) VALUES (0, 'System');

INSERT INTO users (id, username, plaintext_password, enabled) VALUES (0, 'system', '0000', TRUE);
INSERT INTO organisation_members (organisation_id, user_id) VALUES (0, 0);
INSERT INTO user_roles (user_id, role) VALUES (0, 'SUPERUSER');
INSERT INTO user_roles (user_id, role) VALUES (0, 'AGENT');
INSERT INTO user_roles (user_id, role) VALUES (0, 'ADMIN');
INSERT INTO user_roles (user_id, role) VALUES (0, 'USER');
INSERT INTO user_roles (user_id, role) VALUES (0, 'MONITOR');

INSERT INTO users (id, username, plaintext_password, enabled) VALUES (1, 'monitor', '0000', TRUE);
INSERT INTO organisation_members (organisation_id, user_id) VALUES (0, 1);
INSERT INTO user_roles (user_id, role) VALUES (1, 'USER');
INSERT INTO user_roles (user_id, role) VALUES (1, 'MONITOR');

-- Demo account ---------------------------------------------------------------------------
INSERT INTO organisations (id, name) VALUES (1, 'Demo');

INSERT INTO users (id, username, plaintext_password, enabled) VALUES (2, 'agent', '0000', TRUE);
INSERT INTO organisation_members (organisation_id, user_id) VALUES (1, 2);
INSERT INTO user_roles (user_id, role) VALUES (2, 'AGENT');

INSERT INTO users (id, username, plaintext_password, enabled) VALUES (3, 'admin', '0000', TRUE);
INSERT INTO organisation_members (organisation_id, user_id) VALUES (1, 3);
INSERT INTO user_roles (user_id, role) VALUES (3, 'ADMIN');
INSERT INTO user_roles (user_id, role) VALUES (3, 'USER');

INSERT INTO users (id, username, plaintext_password, enabled, email_address) VALUES (4, 'user', '0000', TRUE, 'user@demo.com');
INSERT INTO organisation_members (organisation_id, user_id) VALUES (1, 4);
INSERT INTO user_roles (user_id, role) VALUES (4, 'USER');

INSERT INTO users (id, username, plaintext_password, enabled, email_address) VALUES (5, 'guest', '0000', TRUE, 'guest@demo.com');
INSERT INTO organisation_members (organisation_id, user_id) VALUES (1, 5);
INSERT INTO user_roles (user_id, role) VALUES (5, 'USER');
