DELETE FROM applications;
INSERT INTO applications (id, name, version) VALUES
  (11, 'app1', 'v1'),
  (12, 'app1', 'v2'),
  (21, 'app2', 'v1'),
  (22, 'app2', 'v2');

DELETE FROM methods;
INSERT INTO methods (id, visibility, signature) VALUES
  (1, 'public', 'm1'),
  (2, 'public', 'm2'),
  (3, 'public', 'm3'),
  (4, 'public', 'm4'),
  (5, 'public', 'm5'),
  (6, 'public', 'm6'),
  (7, 'public', 'm7'),
  (8, 'public', 'm8'),
  (9, 'public', 'm9'),
  (10, 'public', 'm10');

DELETE FROM jvms;
INSERT INTO jvms (id, uuid, collectorResolutionSeconds, methodVisibility, packages, excludePackages,
                  collectorComputerId, collectorHostname, collectorVersion, collectorVcsId, tags)
VALUES
  (1, 'uuid1', 600, 'public', 'com.foobar', 'com.foobar.excluded', 'computerId', 'hostname', 'version', 'vcsId', 'tag1=t1,tag2=t2'),
  (2, 'uuid2', 600, 'public', 'com.foobar', 'com.foobar.excluded', 'computerId', 'hostname', 'version', 'vcsId', 'tag1=t1,tag2=t2'),
  (3, 'uuid3', 600, 'public', 'com.foobar', 'com.foobar.excluded', 'computerId', 'hostname', 'version', 'vcsId', 'tag1=t1,tag2=t2'),
  (4, 'uuid4', 600, 'public', 'com.foobar', 'com.foobar.excluded', 'computerId', 'hostname', 'version', 'vcsId', 'tag1=t1,tag2=t2');
