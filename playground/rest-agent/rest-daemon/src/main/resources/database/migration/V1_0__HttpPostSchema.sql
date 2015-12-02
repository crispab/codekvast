CREATE TABLE signatures (
  jvm_uuid               VARCHAR(40) NOT NULL,
  signature              VARCHAR     NOT NULL,
  invoked_at_millis      BIGINT      NOT NULL,
  millis_since_jvm_start BIGINT      NOT NULL,
  confidence             TINYINT     NOT NULL,
  PRIMARY KEY (jvm_uuid, signature)
);
