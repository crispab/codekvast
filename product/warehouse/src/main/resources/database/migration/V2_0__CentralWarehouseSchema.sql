--- file imports --------------------------------
--- Used for making file import idempotent as well as providing some
--- statistics
CREATE TABLE file_imports (
  id                         BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  uuid                       VARCHAR(40)           NOT NULL UNIQUE,
  lengthBytes                BIGINT                NOT NULL,
  importedFromDaemonHostname VARCHAR(80)           NOT NULL,
  importedAt                 TIMESTAMP             NOT NULL
)
  ENGINE = innodb, CHARACTER SET = utf8, COLLATE = utf8_general_ci;

--- applications --------------------------------
CREATE TABLE applications (
  id        BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name      VARCHAR(255)          NOT NULL,
  version   VARCHAR(80)           NOT NULL,
  createdAt TIMESTAMP             NOT NULL,

  CONSTRAINT ix_application_identity UNIQUE KEY (NAME, version)
)
  ENGINE = innodb, CHARACTER SET = utf8, COLLATE = utf8_general_ci;

--- methods --------------------------------
CREATE TABLE methods (
  id             BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  visibility     VARCHAR(20)           NOT NULL,
  signature      TEXT                  NOT NULL,
  createdAt      TIMESTAMP             NOT NULL,
  declaringType  TEXT                  NULL,
  exceptionTypes TEXT                  NULL,
  methodName     TEXT                  NULL,
  modifiers      VARCHAR(50)           NULL,
  packageName    TEXT                  NULL,
  parameterTypes TEXT                  NULL,
  returnType     TEXT                  NULL,

  INDEX ix_method_signature (signature(255))
)
  ENGINE = innodb, CHARACTER SET = utf8, COLLATE = utf8_general_ci;

--- JVMs --------------------------------
CREATE TABLE jvms (
  id          BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  uuid        VARCHAR(40)           NOT NULL UNIQUE,
  startedAt   TIMESTAMP             NOT NULL,
  dumpedAt    TIMESTAMP             NOT NULL,
  jvmDataJson VARCHAR(1000)         NOT NULL
  COMMENT 'jvmDataJson is an instance of se.crisp.codekvast.agent.lib.model.v1.JvmData as JSON'
)
  ENGINE = innodb, CHARACTER SET = utf8, COLLATE = utf8_general_ci;

--- invocations --------------------------------
CREATE TABLE invocations (
  applicationId   BIGINT    NOT NULL,
  methodId        BIGINT    NOT NULL,
  jvmId           BIGINT    NOT NULL,
  invokedAt       TIMESTAMP NOT NULL,
  invocationCount BIGINT    NOT NULL,
  confidence      TINYINT   NULL,

  CONSTRAINT ix_invocation_applicationId FOREIGN KEY (applicationId) REFERENCES applications (id),
  CONSTRAINT ix_invocation_methodId FOREIGN KEY (methodId) REFERENCES methods (id),
  CONSTRAINT ix_invocation_jvmId FOREIGN KEY (jvmId) REFERENCES jvms (id),

  CONSTRAINT ix_invocation_identity UNIQUE KEY (applicationId, methodId, jvmId)
)
  ENGINE = innodb, CHARACTER SET = utf8, COLLATE = utf8_general_ci;
