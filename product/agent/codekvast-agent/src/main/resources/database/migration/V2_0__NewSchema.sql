CREATE TABLE jvms (
  jvm_uuid  VARCHAR(50)   NOT NULL PRIMARY KEY,
  json_data VARCHAR(2000) NOT NULL
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
