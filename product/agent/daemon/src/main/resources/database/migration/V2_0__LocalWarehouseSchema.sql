// --- applications --------------------------------
CREATE TABLE applications (
  id                BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name              VARCHAR(100)          NOT NULL,
  version           VARCHAR(100)          NOT NULL,
  created_at_millis BIGINT                NOT NULL
);
CREATE UNIQUE INDEX ix_application_identity ON applications (name, version);

// --- methods --------------------------------
CREATE TABLE methods (
  id                BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  signature         VARCHAR(4000)         NOT NULL UNIQUE,
  created_at_millis BIGINT                NOT NULL
);

// --- JVMs --------------------------------
CREATE TABLE jvms (
  id                BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  uuid              VARCHAR(50)           NOT NULL UNIQUE,
  started_at_millis BIGINT                NOT NULL,
  dumped_at_millis  BIGINT                NOT NULL,
  json_data         VARCHAR(2000)         NOT NULL
);

// --- invocations --------------------------------
CREATE TABLE invocations (
  application_id     BIGINT  NOT NULL,
  method_id          BIGINT  NOT NULL,
  jvm_id             BIGINT  NOT NULL,
  invoked_at_millis  BIGINT  NOT NULL,
  confidence         TINYINT NULL,
  exported_at_millis BIGINT  NULL,

  FOREIGN KEY (application_id) REFERENCES applications (id),
  FOREIGN KEY (method_id) REFERENCES methods (id),
  FOREIGN KEY (jvm_id) REFERENCES jvms (id)
);
CREATE UNIQUE INDEX ix_invocation_identity ON invocations (application_id, method_id, jvm_id);
