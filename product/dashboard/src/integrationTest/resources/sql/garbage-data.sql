-- Relies on base-data.sql

-- Disable automatic garbage collection for customer 1
DELETE
    FROM price_plan_overrides;

INSERT INTO price_plan_overrides (id, customerId, createdAt, createdBy, updatedAt, note, retentionPeriodDays)
    VALUES (1, 1, NOW(), 'integration test', NOW(), 'Inserted by garbage-data.sql', -1);

INSERT INTO applications(id, customerId, name) VALUES (5, 1, 'app5');

INSERT INTO environments(id, customerId, name, enabled) VALUES (5, 1, 'env5', TRUE);

INSERT INTO jvms(id, customerId, applicationId, applicationVersion, environmentId, uuid, methodVisibility, packages, excludePackages,
                 computerId, hostname, agentVersion, tags, garbage)
VALUES (5, 1, 5, 'v4', 5, 'uuid5', 'private', 'com.foobar5', NULL, 'computerId5', 'hostname5', 'agentVersion5', 'tag1=t1,tag2=t2', TRUE);

INSERT INTO agent_state(id, customerId, jvmUuid, enabled, garbage) VALUES (5, 1, 'uuid5', FALSE, TRUE);

DELETE
    FROM invocations;

INSERT INTO invocations(customerId, applicationId, environmentId, methodId, jvmId, invokedAtMillis, invocationCount, status)
VALUES (1, 1, 1, 1, 1, unix_timestamp(), 1, 'INVOKED'),
       (1, 5, 5, 1, 5, unix_timestamp(), 1, 'INVOKED');
