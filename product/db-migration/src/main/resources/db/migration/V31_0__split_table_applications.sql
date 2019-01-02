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

-- -----------------------------------------------------------------------------------------------------------------------------
-- Move application.version to jvms.applicationVersion
-- -----------------------------------------------------------------------------------------------------------------------------
ALTER TABLE jvms
  ADD COLUMN applicationVersion VARCHAR(80)
  AFTER applicationId;

UPDATE jvms j1
  INNER JOIN jvms j2 ON j1.id = j2.id
  INNER JOIN applications a ON j2.applicationId = a.id
SET j1.applicationVersion = a.version;

-- -----------------------------------------------------------------------------------------------------------------------------
-- Create new version-less rows in applications using the first created version
-- -----------------------------------------------------------------------------------------------------------------------------
CREATE TEMPORARY TABLE tmp SELECT * FROM applications;

INSERT IGNORE INTO applications SELECT 0, tmp.customerId, tmp.name, "", tmp.createdAt FROM tmp ORDER BY tmp.createdAt;

DROP TEMPORARY TABLE tmp;

-- -----------------------------------------------------------------------------------------------------------------------------
-- Update jvms to point to version-less application rows
-- -----------------------------------------------------------------------------------------------------------------------------
UPDATE jvms j1
  INNER JOIN jvms j2 ON j1.id = j2.id
  INNER JOIN applications a1 ON j2.applicationId = a1.id
  INNER JOIN applications a2 ON a1.customerId = a2.customerId AND a1.name = a2.name AND a2.version = ''
SET j1.applicationId = a2.id;

-- -----------------------------------------------------------------------------------------------------------------------------
-- Update invocations to point to version-less application rows
-- -----------------------------------------------------------------------------------------------------------------------------
UPDATE invocations i1
  INNER JOIN invocations i2 ON i1.id = i2.id
  INNER JOIN applications a1 ON i2.applicationId = a1.id
  INNER JOIN applications a2 ON a1.customerId = a2.customerId AND a1.name = a2.name AND a2.version = ''
SET i1.applicationId = a2.id;

-- -----------------------------------------------------------------------------------------------------------------------------
-- Delete versioned rows in applications
-- -----------------------------------------------------------------------------------------------------------------------------
DELETE FROM applications WHERE version <> '';

-- -----------------------------------------------------------------------------------------------------------------------------
-- Drop foreign keys to applications
-- -----------------------------------------------------------------------------------------------------------------------------

ALTER TABLE jvms
  DROP FOREIGN KEY ix_jvm_applicationId;

ALTER TABLE invocations
  DROP FOREIGN KEY ix_invocation_applicationId;

-- -----------------------------------------------------------------------------------------------------------------------------
-- Drop column applications.version
-- -----------------------------------------------------------------------------------------------------------------------------
ALTER TABLE applications
  DROP KEY ix_application_identity,
  DROP COLUMN version,
  ADD CONSTRAINT ix_application_identity UNIQUE (customerId, name);

-- -----------------------------------------------------------------------------------------------------------------------------
-- Add new foreign keys on jvms and invocations
-- -----------------------------------------------------------------------------------------------------------------------------

ALTER TABLE jvms
  ADD FOREIGN KEY ix_jvm_applicationId(applicationId) REFERENCES applications (id);

ALTER TABLE invocations
  ADD FOREIGN KEY ix_invocation_applicationId(applicationId) REFERENCES applications (id);
