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

SET FOREIGN_KEY_CHECKS = 0;

-- Remove duplicate method_locations (keep the oldest)
DELETE ml1
    FROM method_locations       ml1
             INNER JOIN methods m1 ON ml1.methodId = m1.id
             INNER JOIN methods m2
    WHERE m2.id > m1.id AND m1.signature = m2.signature;

-- Remove duplicate invocations (keep the oldest)
DELETE i1
    FROM invocations            i1
             INNER JOIN methods m1 ON i1.methodId = m1.id
             INNER JOIN methods m2
    WHERE m2.id > m1.id AND m1.signature = m2.signature;

-- Remove duplicate methods (keep the oldest)
DELETE m1
    FROM methods                m1
             INNER JOIN methods m2
    WHERE m1.id > m2.id AND m1.signature = m2.signature;

SET FOREIGN_KEY_CHECKS = 1;

-- Now prevent duplicates from appearing again
-- (We can do this now since imports are done with a lock).
ALTER TABLE methods
    MODIFY signature VARCHAR(3000) NOT NULL,
    DROP INDEX ix_method_signature,
    ADD UNIQUE INDEX ix_method_identity(customerId, signature);
