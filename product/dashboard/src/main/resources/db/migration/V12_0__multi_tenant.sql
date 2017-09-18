--
-- Copyright (c) 2015-2017 Hallin Information Technology AB
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

CREATE TABLE customers (
  id         BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name       VARCHAR(100)          NOT NULL UNIQUE,
  licenseKey VARCHAR(40)           NOT NULL UNIQUE,
  createdAt  TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO customers (id, name, licenseKey) VALUES (1, 'Codekvast Demo Customer', '');

-- Applications -------------------------------------
ALTER TABLE applications
  ADD COLUMN customerId BIGINT
  AFTER id;

UPDATE applications
SET customerId = 1;

ALTER TABLE applications
  MODIFY COLUMN customerId BIGINT NOT NULL,
  ADD CONSTRAINT ix_application_customerId FOREIGN KEY (customerId) REFERENCES customers (id);

DROP INDEX ix_application_identity
ON applications;

ALTER TABLE applications
  ADD CONSTRAINT ix_application_identity UNIQUE (customerId, name, version);

-- jvms ---------------------------------------------
ALTER TABLE jvms
  ADD COLUMN customerId BIGINT
  AFTER id;

UPDATE jvms
SET customerId = 1;

ALTER TABLE jvms
  MODIFY COLUMN customerId BIGINT NOT NULL,
  ADD CONSTRAINT ix_jvm_customerId FOREIGN KEY (customerId) REFERENCES customers (id);

-- methods ------------------------------------------
ALTER TABLE methods
  ADD COLUMN customerId BIGINT
  AFTER id;

UPDATE methods
SET customerId = 1;

ALTER TABLE methods
  MODIFY COLUMN customerId BIGINT NOT NULL,
  ADD CONSTRAINT ix_method_customerId FOREIGN KEY (customerId) REFERENCES customers (id);

-- invocations ------------------------------------------
ALTER TABLE invocations
  ADD COLUMN customerId BIGINT FIRST;


UPDATE invocations
SET customerId = 1;

ALTER TABLE invocations
  MODIFY COLUMN customerId BIGINT NOT NULL,
  ADD CONSTRAINT ix_invocation_customerId FOREIGN KEY (customerId) REFERENCES customers (id);
