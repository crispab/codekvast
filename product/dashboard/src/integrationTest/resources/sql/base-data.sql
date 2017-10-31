DELETE FROM agent_state;
DELETE FROM users;
DELETE FROM invocations;
DELETE FROM methods;
DELETE FROM jvms;
DELETE FROM applications;
DELETE FROM price_plan_overrides;
DELETE FROM customers;
DELETE FROM price_plans;

INSERT INTO price_plans (name) VALUES ('test'), ('demo');

INSERT INTO customers (id, source, externalId, name, plan, licenseKey) VALUES (1, 'test', 'external-1', 'Demo', 'demo', '');
INSERT INTO customers (id, source, externalId, name, plan, licenseKey) VALUES (2, 'test', 'external-2', 'Demo', 'demo', 'licenseKey2');
INSERT INTO customers (id, source, externalId, name, plan, licenseKey, collectionStartedAt, trialPeriodEndsAt) VALUES (3, 'test', 'external-3', 'Demo', 'demo', 'licenseKey3', '2017-08-21T16:21:19', '2017-09-21T16:21:19');

INSERT INTO price_plan_overrides (id, customerId, createdAt, createdBy, updatedAt, note, maxMethods)
VALUES (1, 1, NOW(), 'integration test', NOW(), 'Inserted by base-data.sql', 100);

INSERT INTO applications (id, customerId, name, version) VALUES
  (11, 1, 'app1', 'v1'),
  (12, 1, 'app2', 'v2'),
  (21, 1, 'app3', 'v3'),
  (22, 1, 'app4', 'v4');

INSERT INTO methods (id, customerId, visibility, signature) VALUES
  (1, 1, 'public', 'm1'),
  (2, 1, 'public', 'm2'),
  (3, 1, 'public', 'm3'),
  (4, 1, 'public', 'm4'),
  (5, 1, 'public', 'm5'),
  (6, 1, 'public', 'm6'),
  (7, 1, 'public', 'm7'),
  (8, 1, 'public', 'm8'),
  (9, 1, 'public', 'm9'),
  (10, 1, 'public', 'm10');

INSERT INTO jvms (id, customerId, applicationId, uuid, methodVisibility, packages, excludePackages,
                  computerId, hostname, agentVersion, tags, environment)
VALUES
  (1, 1, 11, 'uuid1', 'public', 'com.foobar1', 'com.foobar.excluded1', 'computerId1', 'hostname1', 'agentVersion1', 'tag1=t1,tag2=t2',
   'env1'),
  (2, 1, 12, 'uuid2', 'protected', 'com.foobar2', 'com.foobar.excluded2', 'computerId2', 'hostname2', 'agentVersion2', 'tag1=t1,tag2=t2',
   'env2'),
  (3, 1, 21, 'uuid3', 'package-private', 'com.foobar3', 'com.foobar.excluded3', 'computerId3', 'hostname3', 'agentVersion3',
      'tag1=t1,tag2=t2', 'env3'),
  (4, 1, 22, 'uuid4', 'private', 'com.foobar4', NULL, 'computerId4', 'hostname4', 'agentVersion4', 'tag1=t1,tag2=t2', 'env4');

INSERT INTO agent_state (customerId, jvmUuid, enabled)
VALUES
  (1, 'uuid1', TRUE),
  (1, 'uuid2', FALSE),
  (1, 'uuid3', TRUE),
  (1, 'uuid4', FALSE);

INSERT INTO users (customerId, email, lastLoginSource, numberOfLogins, firstLoginAt, lastLoginAt, lastActivityAt)
VALUES
  (1, 'email1', 'source1', 1, NOW(), NOW(), NOW()),
  (1, 'email2', 'source2', 2, NOW(), NOW(), NOW());

