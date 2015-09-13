CREATE TABLE jvms (
  jvm_uuid                     VARCHAR(50)   NOT NULL PRIMARY KEY,
  application_name             VARCHAR(255)  NOT NULL,
  application_version          VARCHAR(255)  NOT NULL,
  computer_id                  VARCHAR(50)   NOT NULL,
  host_name                    VARCHAR(255)  NOT NULL,
  environment                  VARCHAR(255)  NOT NULL,
  tags                         VARCHAR(1000) NULL,
  collector_resolution_seconds INTEGER       NOT NULL,
  collector_version            VARCHAR(20)   NOT NULL,
  collector_vcs_id             VARCHAR(20)   NOT NULL,
  method_visibility            VARCHAR(50)   NOT NULL,
  package_prefixes             VARCHAR(50)   NOT NULL,
  started_at_millis            BIGINT        NOT NULL,
  dumped_at_millis             BIGINT        NOT NULL,
);

CREATE TABLE methods (
  id        BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  signature VARCHAR(4000)         NOT NULL UNIQUE
);

CREATE TABLE invocations (
  method_id          BIGINT      NOT NULL,
  jvm_uuid           VARCHAR(40) NOT NULL,
  invoked_at_millis  BIGINT      NOT NULL,
  confidence         TINYINT     NULL,
  uploaded_at_millis BIGINT      NOT NULL,

  FOREIGN KEY (method_id) REFERENCES methods (id),
  FOREIGN KEY (jvm_uuid) REFERENCES jvms (jvm_uuid)
)
