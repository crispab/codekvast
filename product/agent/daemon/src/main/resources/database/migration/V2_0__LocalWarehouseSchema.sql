// --- applications --------------------------------
CREATE TABLE applications (
  id              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name            VARCHAR(100)          NOT NULL,
  version         VARCHAR(100)          NOT NULL,
  createdAtMillis BIGINT                NOT NULL
);
CREATE UNIQUE INDEX ix_application_identity ON applications (name, version);

// --- methods --------------------------------
CREATE TABLE methods (
  id              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  visibility      VARCHAR(20)           NOT NULL,
  signature       VARCHAR(4000)         NOT NULL,
  createdAtMillis BIGINT                NOT NULL
);
CREATE UNIQUE INDEX ix_method_identity ON methods (visibility, signature);

// --- JVMs --------------------------------
CREATE TABLE jvms (
  id              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  uuid            VARCHAR(50)           NOT NULL UNIQUE,
  startedAtMillis BIGINT                NOT NULL,
  dumpedAtMillis  BIGINT                NOT NULL,
  jsonData        VARCHAR(2000)         NOT NULL
);

// --- invocations --------------------------------
CREATE TABLE invocations (
  applicationId    BIGINT  NOT NULL,
  methodId         BIGINT  NOT NULL,
  jvmId            BIGINT  NOT NULL,
  invokedAtMillis  BIGINT  NOT NULL,
  invocationCount  BIGINT  NOT NULL,
  confidence       TINYINT NULL,
  exportedAtMillis BIGINT  NULL,

  FOREIGN KEY (applicationId) REFERENCES applications (id),
  FOREIGN KEY (methodId) REFERENCES methods (id),
  FOREIGN KEY (jvmId) REFERENCES jvms (id)
);
CREATE UNIQUE INDEX ix_invocation_identity ON invocations (applicationId, methodId, jvmId);
