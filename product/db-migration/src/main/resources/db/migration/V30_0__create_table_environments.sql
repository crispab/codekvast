--
-- Copyright (c) 2015-2018 Hallin Information Technology AB
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in
-- all copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
-- THE SOFTWARE.
--

CREATE TABLE environments (
  id         BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  customerId BIGINT                NOT NULL,
  name       VARCHAR(255)          NOT NULL,
  createdAt  TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT ix_environment_customerId FOREIGN KEY (customerId) REFERENCES customers (id),
  CONSTRAINT ix_environment_identity UNIQUE (customerId, name)
);

UPDATE jvms SET environment = '<default>' WHERE environment = '';

INSERT INTO environments (customerId, name) SELECT DISTINCT customerId, environment FROM jvms;

ALTER TABLE jvms
  ADD COLUMN environmentId BIGINT AFTER applicationId;

UPDATE jvms j1
  INNER JOIN jvms j2 ON j1.id = j2.id
  INNER JOIN environments e ON e.customerId = j2.customerId AND e.name = j2.environment
SET j1.environmentId = e.id;

ALTER TABLE jvms
  MODIFY COLUMN environmentId BIGINT NOT NULL,
  DROP COLUMN environment;

ALTER TABLE jvms
  ADD CONSTRAINT ix_jvm_environmentId FOREIGN KEY (environmentId) REFERENCES environments (id);

ALTER TABLE invocations
  ADD COLUMN id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY FIRST,
  ADD COLUMN environmentId BIGINT AFTER applicationId;

UPDATE invocations i1
  INNER JOIN invocations i2 ON i1.id = i2.id
  INNER JOIN jvms j ON i2.jvmId = j.id
SET i1.environmentId = j.environmentId;

ALTER TABLE invocations
  MODIFY COLUMN environmentId BIGINT NOT NULL;

ALTER TABLE invocations
  ADD CONSTRAINT ix_invocation_environmentId FOREIGN KEY (environmentId) REFERENCES environments (id);
