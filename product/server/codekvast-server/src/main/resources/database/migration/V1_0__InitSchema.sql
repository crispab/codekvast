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
  id                  BIGINT                              NOT NULL AUTO_INCREMENT PRIMARY KEY,
  organisation_id     BIGINT                              NOT NULL REFERENCES organisations (id),
  name                VARCHAR(100)                        NOT NULL,
  usage_cycle_seconds INTEGER                             NOT NULL
  COMMENT 'After how long can an unused signature in this application be considered dead?',
  notes               VARCHAR(3000)                       NULL
  COMMENT 'Free text notes about the application',
  created_at          TIMESTAMP DEFAULT current_timestamp NOT NULL,
  modified_at         TIMESTAMP AS NOW()
);

CREATE UNIQUE INDEX ix_applications ON applications (organisation_id, name);

CREATE TABLE application_statistics (
  application_id             BIGINT       NOT NULL REFERENCES applications (id),
  application_version        VARCHAR(100) NOT NULL,
  num_signatures             INTEGER      NOT NULL
  COMMENT 'The total number of signatures in this application, invoked or not',
  num_not_invoked_signatures INTEGER      NOT NULL
  COMMENT 'The number of signatures that never has been invoked',
  num_invoked_signatures     INTEGER      NOT NULL
  COMMENT 'The number of invoked signatures in this application',
  num_startup_signatures     INTEGER      NOT NULL
  COMMENT 'The number of signatures that are only invoked within a short time after the application starts',
  num_truly_dead_signatures  INTEGER      NOT NULL
  COMMENT 'The number of truly dead signatures in the application, i.e., never invoked at all or only invoked before the latest
   full usage cycle',
  avg_up_time_millis      BIGINT NOT NULL
  COMMENT 'How many millis has this application version been running in total (average over all instances)?',
  min_up_time_millis      BIGINT NOT NULL
  COMMENT 'How many millis has this application version been running in total (minimum over all instances)?',
  max_up_time_millis      BIGINT NOT NULL
  COMMENT 'How many millis has this application version been running in total (maximum over all instances)?',
  first_started_at_millis BIGINT NOT NULL
  COMMENT 'When was this application version first started?',
  last_reported_at_millis BIGINT NOT NULL
  COMMENT 'When was the last time data was received from this application version?'
);

CREATE PRIMARY KEY ON application_statistics (application_id, application_version);

-- JVM info -------------------------------------------------------------------------------------------------
CREATE TABLE jvm_info (
  id                                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  organisation_id                    BIGINT        NOT NULL REFERENCES organisations (id),
  application_id                     BIGINT        NOT NULL REFERENCES applications (id),
  application_version                VARCHAR(100)  NOT NULL,
  jvm_uuid                           VARCHAR(50)   NOT NULL
  COMMENT 'The UUID generated by each Codekvast Collector instance',
  agent_computer_id                  VARCHAR(50)   NOT NULL
  COMMENT 'The se.crisp.codekvast.agent.util.ComputerID value generated by the Codekvast Agent',
  agent_host_name                    VARCHAR(255)  NOT NULL
  COMMENT 'The hostname of the machine in which Codekvast Agent executes',
  agent_clock_skew_millis            BIGINT        NOT NULL
  COMMENT 'The value of System.currentTimeMillis() in the agent when the latest JVM info was sent',
  agent_upload_interval_seconds      INTEGER       NOT NULL
  COMMENT 'The interval between uploads from this agent (seconds)',
  agent_vcs_id                       VARCHAR(50)   NOT NULL
  COMMENT 'The Git hash of Codekvast Agent used for uploading the data',
  agent_version                      VARCHAR(20)   NOT NULL
  COMMENT 'The version of Codekvast Agent used for uploading the data',
  collector_computer_id              VARCHAR(50)   NOT NULL
  COMMENT 'The se.crisp.codekvast.agent.util.ComputerID value generated by the Codekvast Collector',
  collector_host_name                VARCHAR(255)  NOT NULL
  COMMENT 'The hostname of the machine in which Codekvast Collector executes',
  collector_resolution_seconds       INTEGER       NOT NULL,
  collector_vcs_id                   VARCHAR(50)   NOT NULL
  COMMENT 'The Git hash of Codekvast used for collecting the data',
  collector_version                  VARCHAR(20)   NOT NULL
  COMMENT 'The version of Codekvast used for collecting the data',
  method_visibility                  VARCHAR(50)   NOT NULL
  COMMENT 'Which methods are being tracked?',
  started_at_millis                  BIGINT        NOT NULL
  COMMENT 'The value of System.currentTimeMillis() when Codekvast Collector instance was started',
  reported_at_millis                 BIGINT        NOT NULL
  COMMENT 'The value of System.currentTimeMillis() when Codekvast Collector made an output of the collected data',
  next_report_expected_before_millis BIGINT        NOT NULL
  COMMENT 'The timestamp before next report is expected',
  tags                               VARCHAR(1000) NULL
  COMMENT 'Any tags that were set in codekvast-collector.conf'
);
COMMENT ON TABLE jvm_info IS 'Data about one JVM that is instrumented by the Codekvast Collector';

CREATE UNIQUE INDEX ix_jvm_info ON jvm_info (organisation_id, application_id, jvm_uuid);

-- Environments -------------------------------------------------------------------------------------------------
CREATE TABLE environments (
  id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  organisation_id BIGINT       NOT NULL REFERENCES organisations (id),
  name            VARCHAR(100) NOT NULL
);
COMMENT ON TABLE environments IS 'Groups hostnames into environments';

CREATE UNIQUE INDEX ix_environments ON environments (organisation_id, name);

CREATE TABLE environment_hostnames (
  id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  organisation_id BIGINT       NOT NULL REFERENCES organisations (id),
  environment_id  BIGINT       NOT NULL REFERENCES environments (id),
  host_name       VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX ix_environment_hostnames ON environment_hostnames (organisation_id, environment_id, host_name);

-- Signatures -----------------------------------------------------------------------------------------------
CREATE TABLE signatures (
  organisation_id        BIGINT        NOT NULL REFERENCES organisations (id),
  application_id         BIGINT        NOT NULL REFERENCES applications (id),
  jvm_id                 BIGINT        NOT NULL REFERENCES jvm_info (id),
  signature VARCHAR(4000) NOT NULL
  COMMENT 'The method signature in human readable format',
  invoked_at_millis      BIGINT        NOT NULL
  COMMENT 'The value of System.currentTimeMillis() the method was invoked (rounded to nearest collection interval). 0 means not yet invoked',
  millis_since_jvm_start BIGINT        NOT NULL
  COMMENT 'The delta between invoked_at_millis and the instant the JVM started',
  confidence             TINYINT       NULL
  COMMENT 'The ordinal for se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence. NULL for not yet invoked.'
);

CREATE PRIMARY KEY ON signatures (organisation_id, application_id, jvm_id, signature);
CREATE INDEX signatures_invoked_at_ix ON signatures (invoked_at_millis);

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
