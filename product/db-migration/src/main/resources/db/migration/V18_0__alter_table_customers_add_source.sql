--
-- Copyright (c) 2015-2019 Hallin Information Technology AB
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

ALTER TABLE customers
  ADD COLUMN source VARCHAR(20) NULL
  AFTER externalId;

UPDATE customers
SET source = 'demo'
WHERE id = 1;
UPDATE customers
SET source = 'heroku'
WHERE id <> 1;

ALTER TABLE customers
  MODIFY COLUMN source VARCHAR(20) NOT NULL;

CREATE TABLE users (
  id              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  customerId      BIGINT                NOT NULL,
  email           VARCHAR(100)          NOT NULL,
  firstLoginAt    TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastLoginAt     TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  lastLoginSource VARCHAR(20)           NOT NULL,
  numberOfLogins  INT                   NOT NULL,

  CONSTRAINT ix_user_customerId FOREIGN KEY (customerId) REFERENCES customers (id),
  CONSTRAINT ix_user_identity UNIQUE (customerId, email)
);
