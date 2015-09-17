CREATE TABLE signatures (
  jvm_uuid               VARCHAR(40)   NOT NULL,
  signature              VARCHAR(4000) NOT NULL,
  invoked_at_millis      BIGINT        NOT NULL,
  millis_since_jvm_start BIGINT        NOT NULL,
  confidence             TINYINT       NULL,
  PRIMARY KEY (jvm_uuid, signature)
);
