--
-- Copyright (c) 2015-2021 Hallin Information Technology AB
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

CREATE TABLE price_plans (
    id   BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    name VARCHAR(40) NOT NULL,

    CONSTRAINT ix_price_plans_name
        UNIQUE (name)
);

CREATE TABLE customers (
    id                  BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    externalId          VARCHAR(100)                          NULL,
    source              VARCHAR(20)                           NOT NULL,
    name                VARCHAR(100)                          NOT NULL,
    licenseKey          VARCHAR(40)                           NOT NULL,
    plan                VARCHAR(40)                           NULL,
    createdAt           TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL,
    updatedAt           TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL ON UPDATE CURRENT_TIMESTAMP(),
    contactEmail        VARCHAR(80)                           NULL,
    notes               VARCHAR(255)                          NULL,
    collectionStartedAt TIMESTAMP                             NULL,
    trialPeriodEndsAt   TIMESTAMP                             NULL,

    CONSTRAINT ix_customers_identity
        UNIQUE (source, externalId),
    CONSTRAINT ix_customers_licenseKey
        UNIQUE (licenseKey),
    CONSTRAINT ix_customers_plan
        FOREIGN KEY (plan) REFERENCES price_plans (name)
);

CREATE TABLE agent_state (
    id                 BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId         BIGINT                                  NOT NULL,
    jvmUuid            VARCHAR(40)                             NOT NULL,
    createdAt          TIMESTAMP DEFAULT CURRENT_TIMESTAMP()   NOT NULL,
    lastPolledAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP()   NOT NULL ON UPDATE CURRENT_TIMESTAMP(),
    nextPollExpectedAt TIMESTAMP DEFAULT '0000-00-00 00:00:00' NOT NULL,
    timestamp          TIMESTAMP DEFAULT CURRENT_TIMESTAMP()   NOT NULL ON UPDATE CURRENT_TIMESTAMP(),
    enabled            TINYINT(1)                              NOT NULL,
    garbage            TINYINT(1)                              NOT NULL,

    CONSTRAINT ix_agent_state_identity
        UNIQUE (customerId, jvmUuid),
    CONSTRAINT ix_agent_state_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id),
    INDEX ix_agent_state_garbage (garbage)
);

CREATE TABLE applications (
    id         BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId BIGINT                                NOT NULL,
    name       VARCHAR(255)                          NOT NULL,
    createdAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL ON UPDATE CURRENT_TIMESTAMP(),

    CONSTRAINT ix_application_identity
        UNIQUE (customerId, name),
    CONSTRAINT ix_application_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
);

CREATE TABLE codebase_fingerprints (
    id                  BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId          BIGINT                                    NOT NULL,
    applicationId       BIGINT                                    NOT NULL,
    codeBaseFingerprint VARCHAR(200)                              NOT NULL,
    createdAt           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP()  NOT NULL,
    publishedAt         TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT ix_codebase_fingerprints_identity
        UNIQUE (customerId, applicationId, codeBaseFingerprint),
    CONSTRAINT ix_codebase_fingerprints_applicationId
        FOREIGN KEY (applicationId) REFERENCES applications (id)
            ON DELETE CASCADE,
    CONSTRAINT ix_codebase_fingerprints_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
            ON DELETE CASCADE
);

CREATE TABLE environments (
    id         BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId BIGINT                                NOT NULL,
    name       VARCHAR(255)                          NOT NULL,
    createdAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL,
    updatedAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL ON UPDATE CURRENT_TIMESTAMP(),
    updatedBy  VARCHAR(100)                          NULL COMMENT 'Who updated the environment (email)',
    enabled    TINYINT(1)                            NOT NULL,
    notes      VARCHAR(255)                          NULL COMMENT 'Any notes about the environment',
    CONSTRAINT ix_environment_identity
        UNIQUE (customerId, name),
    CONSTRAINT ix_environment_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
);

CREATE TABLE application_descriptors (
    id             BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId     BIGINT                                         NOT NULL,
    applicationId  BIGINT                                         NOT NULL,
    environmentId  BIGINT                                         NOT NULL,
    collectedSince TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3)      NOT NULL,
    collectedTo    TIMESTAMP(3) DEFAULT '0000-00-00 00:00:00.000' NOT NULL,
    CONSTRAINT ix_application_descriptors_identity
        UNIQUE (customerId, applicationId, environmentId),
    CONSTRAINT ix_application_descriptors_applicationId
        FOREIGN KEY (applicationId) REFERENCES applications (id)
            ON DELETE CASCADE,
    CONSTRAINT ix_application_descriptors_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
            ON DELETE CASCADE,
    CONSTRAINT ix_application_descriptors_environmentId
        FOREIGN KEY (environmentId) REFERENCES environments (id)
            ON DELETE CASCADE
);

CREATE TABLE facts (
    id         BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId BIGINT                                NOT NULL,
    type       VARCHAR(75)                           NOT NULL,
    data       VARCHAR(900)                          NOT NULL COMMENT 'JSON representation of a Java object of type type.',
    createdAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL,
    updatedAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL ON UPDATE CURRENT_TIMESTAMP(),

    CONSTRAINT ix_facts_identity
        UNIQUE (customerId, type, data),
    CONSTRAINT ix_facts_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
);

CREATE TABLE heroku_details (
    id           BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId   BIGINT                                NOT NULL,
    callbackUrl  VARCHAR(500)                          NOT NULL COMMENT 'This is the URL to use for accessing the Heroku Partner API for the associated customer',
    accessToken  TEXT                                  NOT NULL COMMENT 'Encrypted value of the OAuth access token',
    refreshToken TEXT                                  NOT NULL COMMENT 'Encrypted value of the OAuth refresh token',
    tokenType    VARCHAR(50)                           NOT NULL COMMENT 'What type of token is this?',
    expiresAt    TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL ON UPDATE CURRENT_TIMESTAMP() COMMENT 'When does the access token expire?',
    createdAt    TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL,

    CONSTRAINT ix_heroku_details_identity
        UNIQUE (customerId),
    CONSTRAINT ix_heroku_details_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
);

CREATE TABLE jvms (
    id                  BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId          BIGINT                                  NOT NULL,
    applicationId       BIGINT                                  NOT NULL,
    applicationVersion  VARCHAR(80)                             NULL,
    environmentId       BIGINT                                  NOT NULL,
    uuid                VARCHAR(40)                             NOT NULL,
    codeBaseFingerprint VARCHAR(200)                            NULL,
    createdAt           TIMESTAMP DEFAULT CURRENT_TIMESTAMP()   NOT NULL,
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

    CONSTRAINT ix_jvm_uuid
        UNIQUE (uuid),
    CONSTRAINT ix_jvm_applicationId
        FOREIGN KEY (applicationId) REFERENCES applications (id),
    CONSTRAINT ix_jvm_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id),
    CONSTRAINT ix_jvm_environmentId
        FOREIGN KEY (environmentId) REFERENCES environments (id),
    INDEX ix_jvm_garbage (garbage),
    INDEX ix_jvms_codeBaseFingerprint (codeBaseFingerprint)
);

CREATE TABLE methods (
    id             BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId     BIGINT                                 NOT NULL,
    visibility     VARCHAR(20)                            NOT NULL,
    signature      VARCHAR(2000) COLLATE utf8_bin         NOT NULL,
    annotation     VARCHAR(100)                           NULL,
    createdAt      TIMESTAMP  DEFAULT CURRENT_TIMESTAMP() NOT NULL ON UPDATE CURRENT_TIMESTAMP(),
    declaringType  VARCHAR(256)                           NULL,
    exceptionTypes TEXT                                   NULL,
    methodName     TEXT                                   NULL,
    bridge         TINYINT(1)                             NULL,
    synthetic      TINYINT(1)                             NULL,
    modifiers      VARCHAR(50)                            NULL,
    packageName    VARCHAR(256)                           NULL,
    parameterTypes TEXT                                   NULL,
    returnType     TEXT                                   NULL,
    garbage        TINYINT(1) DEFAULT 0                   NULL,

    CONSTRAINT ix_methods_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id),
    INDEX ix_methods_declaring_type
        (declaringType),
    INDEX ix_methods_package
        (packageName),
    INDEX ix_methods_signature
        (signature),
    INDEX ix_methods_garbage
        (garbage)
);

CREATE TABLE invocations (
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId       BIGINT                                                                                                                                                             NOT NULL,
    applicationId    BIGINT                                                                                                                                                             NOT NULL,
    environmentId    BIGINT                                                                                                                                                             NOT NULL,
    methodId         BIGINT                                                                                                                                                             NOT NULL,
    invokedAtMillis  BIGINT                                                                                                                                                             NOT NULL,
    status           ENUM ('NOT_INVOKED', 'INVOKED', 'FOUND_IN_PARENT_CLASS', 'NOT_FOUND_IN_CODE_BASE', 'EXCLUDED_BY_PACKAGE_NAME', 'EXCLUDED_BY_VISIBILITY', 'EXCLUDED_SINCE_TRIVIAL') NOT NULL COMMENT 'Same values as SignatureStatus',
    createdAt        TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3)                                                                                                                          NOT NULL,
    lastSeenAtMillis BIGINT                                                                                                                                                             NULL,
    timestamp        TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3)                                                                                                                          NOT NULL ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT ix_invocations_identity
        UNIQUE (customerId, applicationId, environmentId, methodId),
    CONSTRAINT ix_invocations_applicationId
        FOREIGN KEY (applicationId) REFERENCES applications (id)
            ON DELETE CASCADE,
    CONSTRAINT ix_invocations_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
            ON DELETE CASCADE,
    CONSTRAINT ix_invocations_environmentId
        FOREIGN KEY (environmentId) REFERENCES environments (id)
            ON DELETE CASCADE,
    CONSTRAINT ix_invocations_methodId
        FOREIGN KEY (methodId) REFERENCES methods (id)
            ON DELETE CASCADE,
    INDEX ix_invocations_customerId_status
        (customerId, status),
    INDEX ix_invocations_lastSeenAtMillis
        (lastSeenAtMillis)
);

CREATE TABLE method_locations (
    id                BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId        BIGINT                                NOT NULL,
    methodId          BIGINT                                NOT NULL,
    location          VARCHAR(100)                          NOT NULL,
    locationNoVersion VARCHAR(100) AS (REGEXP_REPLACE(location,
                                                      '-(\\d.*|DEV-SNAPSHOT-fat|SNAPSHOT)\\.jar',
                                                      '.jar')) STORED,
    annotation        VARCHAR(100)                          NULL,
    createdAt         TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL ON UPDATE CURRENT_TIMESTAMP(),

    CONSTRAINT ix_method_locations_identity
        UNIQUE (methodId, location),
    CONSTRAINT ix_method_locations_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id),
    CONSTRAINT ix_method_locations_methodId
        FOREIGN KEY (methodId) REFERENCES methods (id)
            ON DELETE CASCADE,
    INDEX ix_method_locations_location
        (location),
    INDEX ix_method_locations_locationNoVersion
        (locationNoVersion)
);

CREATE TABLE packages (
    id         BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId BIGINT                                NOT NULL,
    name       VARCHAR(256)                          NOT NULL,
    annotation VARCHAR(100)                          NULL,
    createdAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL ON UPDATE CURRENT_TIMESTAMP(),

    CONSTRAINT ix_packages_identity
        UNIQUE (customerId, name),
    CONSTRAINT ix_packages_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
            ON DELETE CASCADE
);

CREATE TABLE price_plan_overrides (
    id                     BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId             BIGINT                                NOT NULL,
    createdAt              TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL,
    createdBy              VARCHAR(255)                          NOT NULL COMMENT 'Free text',
    updatedAt              TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL ON UPDATE CURRENT_TIMESTAMP(),
    note                   VARCHAR(255)                          NULL COMMENT 'Comment about the row',
    maxMethods             INT                                   NULL,
    maxNumberOfAgents      INT                                   NULL,
    trialPeriodDays        INT                                   NULL,
    retentionPeriodDays    INT                                   NULL,
    publishIntervalSeconds INT                                   NULL,
    pollIntervalSeconds    INT                                   NULL,
    retryIntervalSeconds   INT                                   NULL,

    CONSTRAINT ix_price_plan_overrides_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
);

CREATE TABLE rabbitmq_message_ids (
    messageId  VARCHAR(80)                           NOT NULL
        PRIMARY KEY,
    receivedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL
);

CREATE TABLE role_names (
    id   BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    name VARCHAR(25) NOT NULL,

    CONSTRAINT ix_role_names_name
        UNIQUE (name)
);

CREATE TABLE roles (
    roleName VARCHAR(25)  NOT NULL,
    email    VARCHAR(100) NOT NULL,

    CONSTRAINT ix_roles_identity
        UNIQUE (roleName, email),
    CONSTRAINT ix_roles_name
        FOREIGN KEY (roleName) REFERENCES role_names (name)
);

CREATE TABLE synthetic_signature_patterns (
    id           BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    pattern      VARCHAR(255) NOT NULL COMMENT 'Parsed by java.util.regexp.Pattern',
    example      VARCHAR(255) NOT NULL COMMENT 'To use in tests',
    errorMessage VARCHAR(255) NULL COMMENT 'as returned by java.util.regexp.Pattern.compile()',
    CONSTRAINT ix_synthetic_signature_patterns_pattern
        UNIQUE (pattern)
);

CREATE TABLE tokens (
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    code             VARCHAR(32)   NOT NULL,
    token            VARCHAR(1000) NOT NULL,
    expiresAtSeconds MEDIUMTEXT    NOT NULL,

    CONSTRAINT ix_tokens_code
        UNIQUE (code)
);

CREATE TABLE truncated_signatures (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId      BIGINT                              NOT NULL,
    signature       TEXT                                NOT NULL,
    signatureHash   VARCHAR(60) AS (MD5(signature)) STORED,
    length          INTEGER                             NOT NULL,
    truncatedLength INTEGER                             NOT NULL,
    createdAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT ix_truncated_signatures_identity UNIQUE (customerId, signatureHash, length),
    INDEX ix_truncated_signatures_customerId (customerId),
    CONSTRAINT ix_truncated_signatures_customerId FOREIGN KEY (customerId) REFERENCES customers (id) ON DELETE CASCADE
);

CREATE TABLE types (
    id         BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId BIGINT                                NOT NULL,
    name       VARCHAR(256)                          NOT NULL,
    annotation VARCHAR(100)                          NULL,
    createdAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL ON UPDATE CURRENT_TIMESTAMP(),

    CONSTRAINT ix_type_identity
        UNIQUE (customerId, name),
    CONSTRAINT ix_type_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
            ON DELETE CASCADE
);

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    customerId      BIGINT                                     NOT NULL,
    email           VARCHAR(100)                               NOT NULL,
    firstLoginAt    TIMESTAMP    DEFAULT '0000-00-00 00:00:00' NOT NULL,
    lastLoginAt     TIMESTAMP    DEFAULT '0000-00-00 00:00:00' NOT NULL,
    lastLoginSource VARCHAR(100) DEFAULT 'none'                NULL,
    numberOfLogins  INT          DEFAULT 0                     NULL,

    CONSTRAINT ix_user_identity
        UNIQUE (customerId, email),
    CONSTRAINT ix_user_customerId
        FOREIGN KEY (customerId) REFERENCES customers (id)
);

-- Reference data -----------------------------------
INSERT INTO price_plans (name)
    VALUES
        ('test'),
        ('demo');

INSERT INTO customers (id, name, externalId, licenseKey, plan, source)
    VALUES
        (1, 'Codekvast Demo Customer', 'demo', '', 'demo', 'demo');

INSERT INTO role_names (name)
    VALUES
        ('ROLE_ADMIN'),
        ('ROLE_CUSTOMER'),
        ('ROLE_USER');

INSERT IGNORE INTO customers(id, licenseKey, source, name, plan, notes)
    VALUES
    (
    -1,
    UUID(),
    'system',
    'no-customer',
    'demo',
    'Owner of facts that are not related to any customer');

INSERT INTO synthetic_signature_patterns (pattern, example)
    VALUES
    (
    '.*\\$\\$.*',
    'customer2.controllers.Events..se$crisp$signup4$controllers$Events$$allGuests(se.crisp.signup4.models.Event)'),
    (
    '.*\\$\\w+\\$.*',
    'controllers.Assets.play$api$http$HeaderNames$_setter_$LOCATION_$eq(java.lang.String)'),
    (
    '.*\\.[A-Z0-9_]+\\(.*\\)$', 'controllers.customer1.application.Admin.CONTENT_MD5()'),
    (
    '.*\\$[a-z]+\\(\\)$', 'controllers.customer1.application.XxxYyy.$amp()'),
    (
    '.*\\.\\.anonfun\\..*',
    'controllers.AssetsBuilder..anonfun.at.1..anonfun.apply.2..anonfun.7.apply()'),
    (
    '.*\\.\\.EnhancerBySpringCGLIB\\.\\..*',
    'customer1.FooConfig..EnhancerBySpringCGLIB..96aac875.CGLIB$BIND_CALLBACKS(java.lang.Object)'),
    (
    '.*\\.\\.FastClassBySpringCGLIB\\.\\..*',
    'customer1.FooConfig..FastClassBySpringCGLIB..73e1cc5a.getIndex(org.springframework.cglib.core.Signature)'),
    (
    '.*\\.canEqual\\(java\\.lang\\.Object\\)',
    'io.codekvast.backoffice.facts.CollectionStarted.canEqual(java.lang.Object)');
