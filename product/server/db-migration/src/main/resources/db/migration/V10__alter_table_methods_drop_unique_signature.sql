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

SELECT name
    FROM internal_locks
    WHERE name = 'IMPORT' FOR
UPDATE;

-- -------------------------------------------------------------------------------------------------------
-- It does not work with a unique index on methods.signature, since MariaDB has a restriction on
-- an index key to be max 3032 bytes.
-- A VARCHAR(2000) in URF-8 could be up to 6000 bytes, so the index needs to be shorter.
-- This means that even if the two signatures are different, they could collide in the index.
--
-- We have to rely on the application to not insert duplicate values.
-- -------------------------------------------------------------------------------------------------------
ALTER TABLE methods
    DROP INDEX ix_method_identity;
