-- Relies on base-data.sql

DELETE FROM invocations;

INSERT INTO applications(id, customerId, name) VALUES (5, 1, 'app5');

INSERT INTO environments(id, customerId, name) VALUES (5, 1, 'env5');

INSERT INTO jvms(id, customerId, applicationId, applicationVersion, environmentId, uuid, methodVisibility, packages, excludePackages,
                 computerId, hostname, agentVersion, tags, garbage)
VALUES (5, 1, 5, 'v4', 5, 'uuid5', 'private', 'com.foobar5', NULL, 'computerId5', 'hostname5', 'agentVersion5', 'tag1=t1,tag2=t2', TRUE);

INSERT INTO agent_state(id, customerId, jvmUuid, enabled, garbage) VALUES (5, 1, 'uuid5', FALSE, TRUE);

INSERT INTO invocations(customerId, applicationId, environmentId, methodId, jvmId, invokedAtMillis, invocationCount, status)
VALUES (1, 1, 1, 1, 1, unix_timestamp(), 1, 'INVOKED'),
       (1, 5, 5, 1, 5, unix_timestamp(), 1, 'INVOKED');
