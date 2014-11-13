//--- People that have shown interest for Codekvast...
DROP TABLE IF EXISTS people;
CREATE TABLE people (
  id            INTEGER                             NOT NULL IDENTITY,
  full_name     VARCHAR(255)                        NOT NULL,
  email_address VARCHAR(255)                        NOT NULL,
  company       VARCHAR(255),
  country       VARCHAR(255),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  modified_at   TIMESTAMP AS NOW(),
  state         VARCHAR(20) DEFAULT 'new'           NOT NULL
);
