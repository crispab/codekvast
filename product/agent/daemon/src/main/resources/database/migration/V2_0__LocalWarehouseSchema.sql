// --- applications --------------------------------
CREATE TABLE applications (
  id              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name            VARCHAR               NOT NULL,
  version         VARCHAR               NOT NULL,
  createdAtMillis BIGINT                NOT NULL
);
CREATE UNIQUE INDEX ix_application_identity ON applications (name, version);

// --- methods --------------------------------
CREATE TABLE methods (
  id              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  visibility      VARCHAR               NOT NULL,
  signature       VARCHAR               NOT NULL UNIQUE,
  createdAtMillis BIGINT                NOT NULL,
  declaringType   VARCHAR               NULL,
  exceptionTypes  VARCHAR               NULL,
  methodName      VARCHAR               NULL,
  modifiers       VARCHAR               NULL,
  packageName     VARCHAR               NULL,
  parameterTypes  VARCHAR               NULL,
  returnType      VARCHAR               NULL
);

// --- JVMs --------------------------------
CREATE TABLE jvms (
  id              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  uuid            VARCHAR               NOT NULL UNIQUE,
  startedAtMillis BIGINT                NOT NULL,
  dumpedAtMillis  BIGINT                NOT NULL,
  jvmDataJson     VARCHAR               NOT NULL
);
COMMENT ON COLUMN jvms.jvmDataJson IS 'An instance of se.crisp.codekvast.daemon.model.v1.JvmData as JSON';

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
