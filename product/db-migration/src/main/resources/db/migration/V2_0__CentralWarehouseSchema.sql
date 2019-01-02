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

-- Import file info --------------------------------
-- Used for making file import idempotent as well as providing some
-- statistics
CREATE TABLE import_file_info (
  id                         BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  uuid                       VARCHAR(40)           NOT NULL UNIQUE,
  daemonVersion              VARCHAR(20)           NOT NULL,
  daemonVcsId                VARCHAR(30)           NOT NULL,
  fileSchemaVersion          VARCHAR(10)           NOT NULL,
  fileName                   VARCHAR(255)          NOT NULL,
  fileLengthBytes            BIGINT                NOT NULL,
  importTimeMillis           BIGINT                NOT NULL,
  importedFromDaemonHostname VARCHAR(80)           NOT NULL,
  importedAt                 TIMESTAMP             NOT NULL,
  importedFromEnvironment    VARCHAR(255)          NULL
);

-- Applications --------------------------------
CREATE TABLE applications (
  id        BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  name      VARCHAR(255)          NOT NULL,
  version   VARCHAR(80)           NOT NULL,
  createdAt TIMESTAMP             NOT NULL,

  CONSTRAINT ix_application_identity UNIQUE (NAME, version)
);

-- Methods --------------------------------
CREATE TABLE methods (
  id             BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  visibility     VARCHAR(20)           NOT NULL,
  signature      TEXT                  NOT NULL,
  createdAt      TIMESTAMP             NOT NULL,
  declaringType  TEXT                  NULL,
  exceptionTypes TEXT                  NULL,
  methodName     TEXT                  NULL,
  modifiers      VARCHAR(50)           NULL,
  packageName    TEXT                  NULL,
  parameterTypes TEXT                  NULL,
  returnType     TEXT                  NULL,

  INDEX ix_method_signature (signature(255)),
  INDEX ix_method_declaring_type (declaringType(255)),
  INDEX ix_method_package (packageName(255))
);

-- JVMs --------------------------------
CREATE TABLE jvms (
  id                         BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  uuid                       VARCHAR(40)           NOT NULL UNIQUE,
  startedAt                  TIMESTAMP             NOT NULL,
  dumpedAt                   TIMESTAMP             NOT NULL,
  collectorResolutionSeconds INT                   NOT NULL,
  methodVisibility           VARCHAR(20)           NOT NULL,
  packagePrefixes            VARCHAR(255)          NOT NULL,
  environment                VARCHAR(255)          NULL,
  collectorComputerId        VARCHAR(40)           NOT NULL,
  collectorHostName          VARCHAR(255)          NOT NULL,
  collectorVersion           VARCHAR(40)           NOT NULL,
  collectorVcsId             VARCHAR(40)           NOT NULL,
  tags                       VARCHAR(1000)         NOT NULL
);

-- invocations --------------------------------
CREATE TABLE invocations (
  applicationId   BIGINT                         NOT NULL,
  methodId        BIGINT                         NOT NULL,
  jvmId           BIGINT                         NOT NULL,
  invokedAtMillis BIGINT                         NOT NULL,
  invocationCount BIGINT                         NOT NULL,
    -- Space optimization: store enum value as one single byte
  confidence      ENUM('NOT_INVOKED',
                       'EXACT_MATCH',
                       'FOUND_IN_PARENT_CLASS',
                       'NOT_FOUND_IN_CODE_BASE') NOT NULL
  COMMENT 'Same values as SignatureStatus',

  CONSTRAINT ix_invocation_applicationId FOREIGN KEY (applicationId) REFERENCES applications (id),
  CONSTRAINT ix_invocation_methodId FOREIGN KEY (methodId) REFERENCES methods (id),
  CONSTRAINT ix_invocation_jvmId FOREIGN KEY (jvmId) REFERENCES jvms (id),

  CONSTRAINT ix_invocation_identity UNIQUE (applicationId, methodId, jvmId)
);
