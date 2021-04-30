SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE agent_state;
TRUNCATE application_descriptors;
TRUNCATE applications;
TRUNCATE codebase_fingerprints;
TRUNCATE customers;
TRUNCATE environments;
TRUNCATE facts;
TRUNCATE heroku_details;
TRUNCATE invocations;
TRUNCATE jvms;
TRUNCATE method_locations;
TRUNCATE methods;
TRUNCATE packages;
TRUNCATE price_plan_overrides;
TRUNCATE price_plans;
TRUNCATE rabbitmq_message_ids;
TRUNCATE role_names;
TRUNCATE roles;
TRUNCATE synthetic_signature_patterns;
TRUNCATE types;
TRUNCATE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO price_plans (name)
    VALUES
        ('test'),
        ('demo');

INSERT INTO customers (
                      id, source, externalId, name, plan, licenseKey, contactEmail)
    VALUES
    (
    1, 'test', 'external-1', 'Demo', 'demo', '', 'contactEmail1');

INSERT INTO customers (id, source, externalId, name, plan, licenseKey)
    VALUES
        (2, 'test', 'external-2', 'Demo', 'demo', 'licenseKey2');

INSERT INTO customers (
                      id,
                      source,
                      externalId,
                      name,
                      plan,
                      licenseKey,
                      collectionStartedAt,
                      trialPeriodEndsAt)
    VALUES
    (
    3,
    'test',
    'external-3',
    'Demo',
    'demo',
    'licenseKey3',
    '2017-08-21T16:21:19',
    '2017-09-21T16:21:19');

INSERT INTO price_plan_overrides (
                                 id, customerId, createdAt, createdBy, updatedAt, note, maxMethods)
    VALUES
    (
    1, 1, NOW(), 'integration test', NOW(), 'Inserted by base-data.sql', 100);

INSERT INTO heroku_details (
                           id,
                           customerId,
                           callbackUrl,
                           accessToken,
                           refreshToken,
                           tokenType,
                           expiresAt)
    VALUES
    (
    1, 1, 'callback', 'accessToken', 'refreshToken', 'tokenType', NOW());

INSERT INTO applications (id, customerId, name)
    VALUES
        (1, 1, 'app1'),
        (2, 1, 'app2'),
        (3, 1, 'app3'),
        (4, 1, 'app4');

INSERT INTO environments (id, customerId, name, enabled)
    VALUES
        (1, 1, 'env1', TRUE),
        (2, 1, 'env2', TRUE),
        (3, 1, 'env3', TRUE),
        (4, 1, 'env4', FALSE);

INSERT INTO packages(id, customerId, name)
    VALUES
        (1, 1, 'p1'),
        (2, 1, 'p2');

INSERT INTO types(id, customerId, name)
    VALUES
        (1, 1, 't1'),
        (2, 1, 't2'),
        (3, 1, 't3'),
        (4, 1, 't4');

INSERT INTO methods (
                    id, customerId, visibility, signature, declaringType, packageName)
    VALUES
        (1, 1, 'public', 'm1', 't1', 'p1'),
        (2, 1, 'public', 'm2', 't2', 'p2'),
        (3, 1, 'public', 'm3', 't3', 'p1'),
        (4, 1, 'public', 'm4', 't4', 'p2'),
        (5, 1, 'public', 'm5', 't1', 'p1'),
        (6, 1, 'public', 'm6', 't2', 'p2'),
        (7, 1, 'public', 'm7', 't3', 'p1'),
        (8, 1, 'public', 'm8', 't4', 'p2'),
        (9, 1, 'public', 'm9', 't1', 'p1'),
        (10, 1, 'public', 'm10', 't2', 'p2');

INSERT INTO method_locations(id, customerId, methodId, location)
    VALUES
        (1, 1, 1, 'loc1'),
        (2, 1, 1, 'loc2'),
        (3, 1, 1, 'loc3'),
        (4, 1, 2, 'loc1'),
        (5, 1, 2, 'loc2'),
        (6, 1, 2, 'loc3');

INSERT INTO jvms (
                 id,
                 customerId,
                 applicationId,
                 applicationVersion,
                 environmentId,
                 uuid,
                 methodVisibility,
                 packages,
                 excludePackages,
                 computerId,
                 hostname,
                 agentVersion,
                 tags,
                 garbage)
    VALUES
    (
    1,
    1,
    1,
    'v1',
    1,
    'uuid1',
    'public',
    'com.foobar1',
    'com.foobar.excluded1',
    'computerId1',
    'hostname1',
    'agentVersion1',
    'tag1=t1,tag2=t2',
    FALSE),
    (
    2,
    1,
    2,
    'v2',
    2,
    'uuid2',
    'protected',
    'com.foobar2',
    'com.foobar.excluded2',
    'computerId2',
    'hostname2',
    'agentVersion2',
    'tag1=t1,tag2=t2',
    FALSE),
    (
    3,
    1,
    3,
    'v3',
    3,
    'uuid3',
    'package-private',
    'com.foobar3',
    'com.foobar.excluded3',
    'computerId3',
    'hostname3',
    'agentVersion3',
    'tag1=t1,tag2=t2',
    FALSE),
    (
    4,
    1,
    4,
    'v4',
    4,
    'uuid4',
    'private',
    'com.foobar4',
    NULL,
    'computerId4',
    'hostname4',
    'agentVersion4',
    'tag1=t1,tag2=t2',
    FALSE);

INSERT INTO agent_state (id, customerId, jvmUuid, enabled, garbage)
    VALUES
        (1, 1, 'uuid1', TRUE, FALSE),
        (2, 1, 'uuid2', FALSE, FALSE),
        (3, 1, 'uuid3', TRUE, FALSE),
        (4, 1, 'uuid4', FALSE, FALSE);

INSERT INTO users (
                  id, customerId, email, lastLoginSource, numberOfLogins, firstLoginAt, lastLoginAt)
    VALUES
        (1, 1, 'email1', 'source1', 1, NOW(), NOW()),
        (2, 1, 'email2', 'source2', 2, NOW(), NOW());

INSERT INTO facts(id, customerId, type, data)
    VALUES
        (1, 1, 'type1', 'data1'),
        (2, 1, 'type2', 'data2');
