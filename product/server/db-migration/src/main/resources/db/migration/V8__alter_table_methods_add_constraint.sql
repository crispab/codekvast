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
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
-- THE SOFTWARE.
--

INSERT IGNORE INTO internal_locks(name) VALUE('IMPORT');
SELECT name FROM internal_locks WHERE name = 'IMPORT' FOR UPDATE;

SET FOREIGN_KEY_CHECKS = 0;

-- Change column type from TEXT to VARCHAR
ALTER TABLE methods
    DROP INDEX IF EXISTS ix_method_signature,
    MODIFY signature VARCHAR(2000) NOT NULL,
    ADD INDEX ix_method_signature(signature(1000));

-- Remove duplicate methods (keep the oldest)
DELETE m1 FROM methods m1 INNER JOIN methods m2
    WHERE m1.signature = m2.signature AND m1.id > m2.id;

-- Remove orphan method_locations
DELETE ml1 FROM method_locations ml1 LEFT JOIN methods m1 ON ml1.methodId = m1.id
    WHERE m1.id IS NULL;

-- Remove orphan invocations
DELETE i1 FROM invocations i1 LEFT JOIN methods m1 ON i1.methodId = m1.id
    WHERE m1.id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE methods
    DROP INDEX ix_method_signature,
    ADD UNIQUE INDEX ix_method_identity(customerId, signature(1000));
