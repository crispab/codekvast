--
-- Copyright (c) 2015 Crisp AB
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

// --- applications --------------------------------
CREATE TABLE applications (
  id              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name            VARCHAR               NOT NULL,
  version         VARCHAR               NOT NULL,
  createdAtMillis BIGINT                NOT NULL
);
CREATE UNIQUE INDEX ix_application_identity ON applications (name, version);

// --- methods --------------------------------
CREATE TABLE methods (
  id              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  visibility      VARCHAR               NOT NULL,
  signature       VARCHAR               NOT NULL UNIQUE,
  createdAtMillis BIGINT                NOT NULL,
  declaringType   VARCHAR               NULL,
  exceptionTypes  VARCHAR               NULL,
  methodName      VARCHAR               NULL,
  modifiers       VARCHAR               NULL,
  packageName     VARCHAR               NULL,
  parameterTypes  VARCHAR               NULL,
  returnType      VARCHAR               NULL
);

// --- JVMs --------------------------------
CREATE TABLE jvms (
  id              BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  uuid            VARCHAR               NOT NULL UNIQUE,
  startedAtMillis BIGINT                NOT NULL,
  dumpedAtMillis  BIGINT                NOT NULL,
  jvmDataJson     VARCHAR               NOT NULL
);
COMMENT ON COLUMN jvms.jvmDataJson IS 'An instance of JvmData as JSON';

// --- invocations --------------------------------
CREATE TABLE invocations (
  applicationId    BIGINT  NOT NULL,
  methodId         BIGINT  NOT NULL,
  jvmId            BIGINT  NOT NULL,
  invokedAtMillis  BIGINT  NOT NULL,
  invocationCount  BIGINT  NOT NULL,
  confidence       TINYINT NOT NULL,

  FOREIGN KEY (applicationId) REFERENCES applications (id),
  FOREIGN KEY (methodId) REFERENCES methods (id),
  FOREIGN KEY (jvmId) REFERENCES jvms (id)
);
CREATE UNIQUE INDEX ix_invocation_identity ON invocations (applicationId, methodId, jvmId);
