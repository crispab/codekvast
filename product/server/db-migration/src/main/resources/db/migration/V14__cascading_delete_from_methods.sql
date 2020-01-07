--
-- Copyright (c) 2015-2020 Hallin Information Technology AB
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

ALTER TABLE methods
    ALGORITHM = INPLACE,
    ADD COLUMN garbage TINYINT(1) NULL DEFAULT 0;

ALTER TABLE methods
    ADD INDEX ix_methods_garbage(garbage);

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE method_locations
    DROP FOREIGN KEY ix_method_location_methodId;

ALTER TABLE method_locations
    ADD CONSTRAINT ix_method_location_methodId FOREIGN KEY (methodId) REFERENCES methods(id) ON DELETE CASCADE;

ALTER TABLE invocations
    DROP FOREIGN KEY ix_invocation_methodId;

ALTER TABLE invocations
    ADD CONSTRAINT ix_invocation_methodId FOREIGN KEY (methodId) REFERENCES methods(id) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;
