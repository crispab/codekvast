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

CREATE TABLE price_plans (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    CONSTRAINT name UNIQUE (name)
);

CREATE TABLE customers (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    externalId          VARCHAR(100)                        NULL,
    source              VARCHAR(20)                         NOT NULL,
    name                VARCHAR(100)                        NOT NULL,
    licenseKey          VARCHAR(40)                         NOT NULL,
    plan                VARCHAR(40)                         NULL,
    createdAt           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updatedAt           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    contactEmail        VARCHAR(80)                         NULL,
    notes               VARCHAR(255)                        NULL,
    collectionStartedAt TIMESTAMP                           NULL,
    trialPeriodEndsAt   TIMESTAMP                           NULL,
    CONSTRAINT externalId UNIQUE (externalId),
    CONSTRAINT licenseKey UNIQUE (licenseKey),
    CONSTRAINT ix_customer_planName FOREIGN KEY (plan) REFERENCES price_plans(name)
);

CREATE TABLE agent_state (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId         BIGINT                                  NOT NULL,
    jvmUuid            VARCHAR(40)                             NOT NULL,
    createdAt          TIMESTAMP DEFAULT CURRENT_TIMESTAMP     NOT NULL,
    lastPolledAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP     NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    nextPollExpectedAt TIMESTAMP DEFAULT '0000-00-00 00:00:00' NOT NULL,
    timestamp          TIMESTAMP DEFAULT CURRENT_TIMESTAMP     NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    enabled            TINYINT(1)                              NOT NULL,
    garbage            TINYINT(1)                              NOT NULL,
    CONSTRAINT ix_agent_state_identity UNIQUE (customerId, jvmUuid),
    CONSTRAINT ix_agent_state_customerId FOREIGN KEY (customerId) REFERENCES customers(id)
);

CREATE INDEX ix_agent_state_garbage ON agent_state(garbage);

CREATE TABLE applications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId BIGINT                              NOT NULL,
    name       VARCHAR(255)                        NOT NULL,
    createdAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ix_application_identity UNIQUE (customerId, name),
    CONSTRAINT ix_application_customerId FOREIGN KEY (customerId) REFERENCES customers(id)
);

CREATE TABLE environments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId BIGINT                              NOT NULL,
    name       VARCHAR(255)                        NOT NULL,
    createdAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updatedAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    updatedBy  VARCHAR(100)                        NULL COMMENT 'Who updated the environment (email)',
    enabled    TINYINT(1)                          NOT NULL,
    notes      VARCHAR(255)                        NULL COMMENT 'Any notes about the environment',
    CONSTRAINT ix_environment_identity UNIQUE (customerId, name),
    CONSTRAINT ix_environment_customerId FOREIGN KEY (customerId) REFERENCES customers(id)
);

CREATE TABLE heroku_details (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId   BIGINT                              NOT NULL,
    callbackUrl  VARCHAR(500)                        NOT NULL COMMENT 'This is the URL to use for accessing the Heroku Partner API for the associated customer',
    accessToken  TEXT                                NOT NULL COMMENT 'Encrypted value of the OAuth access token',
    refreshToken TEXT                                NOT NULL COMMENT 'Encrypted value of the OAuth refresh token',
    tokenType    VARCHAR(50)                         NOT NULL COMMENT 'What type of token is this?',
    expiresAt    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'When does the access token expire?',
    createdAt    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT customerId UNIQUE (customerId),
    CONSTRAINT ix_heroku_details_customerId FOREIGN KEY (customerId) REFERENCES customers(id)
);

CREATE TABLE jvms (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId          BIGINT                                  NOT NULL,
    applicationId       BIGINT                                  NOT NULL,
    applicationVersion  VARCHAR(80)                             NULL,
    environmentId       BIGINT                                  NOT NULL,
    uuid                VARCHAR(40)                             NOT NULL,
    codeBaseFingerprint VARCHAR(200)                            NULL,
    createdAt           TIMESTAMP DEFAULT CURRENT_TIMESTAMP     NOT NULL,
    startedAt           TIMESTAMP DEFAULT '0000-00-00 00:00:00' NOT NULL,
    publishedAt         TIMESTAMP DEFAULT '0000-00-00 00:00:00' NOT NULL,
    methodVisibility    VARCHAR(20)                             NOT NULL,
    packages            VARCHAR(255)                            NOT NULL,
    excludePackages     VARCHAR(255)                            NULL,
    computerId          VARCHAR(40)                             NOT NULL,
    hostname            VARCHAR(255)                            NOT NULL,
    agentVersion        VARCHAR(40)                             NOT NULL,
    tags                VARCHAR(1000)                           NOT NULL,
    garbage             TINYINT(1)                              NOT NULL,
    CONSTRAINT uuid UNIQUE (uuid),
    CONSTRAINT ix_jvm_applicationId FOREIGN KEY (applicationId) REFERENCES applications(id),
    CONSTRAINT ix_jvm_customerId FOREIGN KEY (customerId) REFERENCES customers(id),
    CONSTRAINT ix_jvm_environmentId FOREIGN KEY (environmentId) REFERENCES environments(id)
);

CREATE INDEX ix_jvm_garbage ON jvms(garbage);

CREATE TABLE methods (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId     BIGINT                              NOT NULL,
    visibility     VARCHAR(20)                         NOT NULL,
    signature      TEXT                                NOT NULL,
    createdAt      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    declaringType  TEXT                                NULL,
    exceptionTypes TEXT                                NULL,
    methodName     TEXT                                NULL,
    bridge         TINYINT(1)                          NULL,
    synthetic      TINYINT(1)                          NULL,
    modifiers      VARCHAR(50)                         NULL,
    packageName    TEXT                                NULL,
    parameterTypes TEXT                                NULL,
    returnType     TEXT                                NULL,
    CONSTRAINT ix_method_customerId FOREIGN KEY (customerId) REFERENCES customers(id)
);

CREATE TABLE invocations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId      BIGINT                                                                                                                                                             NOT NULL,
    applicationId   BIGINT                                                                                                                                                             NOT NULL,
    environmentId   BIGINT                                                                                                                                                             NOT NULL,
    methodId        BIGINT                                                                                                                                                             NOT NULL,
    jvmId           BIGINT                                                                                                                                                             NOT NULL,
    invokedAtMillis BIGINT                                                                                                                                                             NOT NULL,
    invocationCount BIGINT                                                                                                                                                             NOT NULL,
    status          ENUM ('NOT_INVOKED', 'INVOKED', 'FOUND_IN_PARENT_CLASS', 'NOT_FOUND_IN_CODE_BASE', 'EXCLUDED_BY_PACKAGE_NAME', 'EXCLUDED_BY_VISIBILITY', 'EXCLUDED_SINCE_TRIVIAL') NOT NULL COMMENT 'Same values as SignatureStatus',
    timestamp       TIMESTAMP DEFAULT CURRENT_TIMESTAMP                                                                                                                                NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ix_invocation_identity UNIQUE (applicationId, methodId, jvmId),
    CONSTRAINT ix_invocation_applicationId FOREIGN KEY (applicationId) REFERENCES applications(id),
    CONSTRAINT ix_invocation_customerId FOREIGN KEY (customerId) REFERENCES customers(id),
    CONSTRAINT ix_invocation_environmentId FOREIGN KEY (environmentId) REFERENCES environments(id),
    CONSTRAINT ix_invocation_jvmId FOREIGN KEY (jvmId) REFERENCES jvms(id) ON DELETE CASCADE,
    CONSTRAINT ix_invocation_methodId FOREIGN KEY (methodId) REFERENCES methods(id)
);

CREATE INDEX ix_invocation_status ON invocations(status);

CREATE INDEX ix_method_declaring_type ON methods(declaringType(255));

CREATE INDEX ix_method_package ON methods(packageName(255));

CREATE INDEX ix_method_signature ON methods(signature(255));

CREATE TABLE price_plan_overrides (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId             BIGINT                              NOT NULL,
    createdAt              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    createdBy              VARCHAR(255)                        NOT NULL COMMENT 'Free text',
    updatedAt              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    note                   VARCHAR(255)                        NULL COMMENT 'Comment about the row',
    maxMethods             INT                                 NULL,
    maxNumberOfAgents      INT                                 NULL,
    trialPeriodDays        INT                                 NULL,
    retentionPeriodDays    INT                                 NULL,
    publishIntervalSeconds INT                                 NULL,
    pollIntervalSeconds    INT                                 NULL,
    retryIntervalSeconds   INT                                 NULL,
    CONSTRAINT ix_pricePlan_customerId FOREIGN KEY (customerId) REFERENCES customers(id)
);

CREATE TABLE role_names (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(25) NOT NULL,
    CONSTRAINT name UNIQUE (name)
);

CREATE TABLE roles (
    roleName VARCHAR(25)  NOT NULL,
    email    VARCHAR(100) NOT NULL,
    CONSTRAINT ix_role_identity UNIQUE (roleName, email),
    CONSTRAINT ix_role_name FOREIGN KEY (roleName) REFERENCES role_names(name)
);

CREATE TABLE tokens (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(32)   NOT NULL,
    token            VARCHAR(1000) NOT NULL,
    expiresAtSeconds MEDIUMTEXT    NOT NULL,
    CONSTRAINT code UNIQUE (code)
);

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId      BIGINT                                     NOT NULL,
    email           VARCHAR(100)                               NOT NULL,
    firstLoginAt    TIMESTAMP    DEFAULT '0000-00-00 00:00:00' NOT NULL,
    lastLoginAt     TIMESTAMP    DEFAULT '0000-00-00 00:00:00' NOT NULL,
    lastLoginSource VARCHAR(100) DEFAULT 'none'                NULL,
    numberOfLogins  INT          DEFAULT 0                     NULL,
    CONSTRAINT ix_user_identity UNIQUE (customerId, email),
    CONSTRAINT ix_user_customerId FOREIGN KEY (customerId) REFERENCES customers(id)
);

-- Reference data -----------------------------------
INSERT INTO price_plans (name)
    VALUES ('test'),
        ('demo');

INSERT INTO customers (id, name, externalId, licenseKey, plan, source)
    VALUES (1, 'Codekvast Demo Customer', 'demo', '', 'demo', 'demo');

INSERT INTO role_names (name)
    VALUES ('ROLE_ADMIN'),
        ('ROLE_CUSTOMER'),
        ('ROLE_USER');
