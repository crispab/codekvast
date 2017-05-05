DELETE FROM invocations;

DELETE FROM customers;
INSERT INTO customers(id, name, licenseKey) VALUES(1, 'Demo', '');

DELETE FROM applications;
INSERT INTO applications (id, customerId, name, version) VALUES
  (11, 1, 'app1', 'v1'),
  (12, 1, 'app1', 'v2'),
  (21, 1, 'app2', 'v1'),
  (22, 1, 'app2', 'v2');

DELETE FROM methods;
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

DELETE FROM jvms;
INSERT INTO jvms (id, customerId, uuid, methodVisibility, packages, excludePackages,
                  computerId, hostname, agentVersion, tags)
VALUES
  (1, 1, 'uuid1', 'public', 'com.foobar', 'com.foobar.excluded', 'computerId', 'hostname', 'version', 'tag1=t1,tag2=t2'),
  (2, 1, 'uuid2', 'public', 'com.foobar', 'com.foobar.excluded', 'computerId', 'hostname', 'version', 'tag1=t1,tag2=t2'),
  (3, 1, 'uuid3', 'public', 'com.foobar', 'com.foobar.excluded', 'computerId', 'hostname', 'version', 'tag1=t1,tag2=t2'),
  (4, 1, 'uuid4', 'public', 'com.foobar', 'com.foobar.excluded', 'computerId', 'hostname', 'version', 'tag1=t1,tag2=t2');
