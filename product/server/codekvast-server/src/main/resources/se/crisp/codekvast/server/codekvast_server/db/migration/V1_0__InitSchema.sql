DROP TABLE IF EXISTS role;
CREATE TABLE role(
    id INTEGER NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL UNIQUE,
);

DROP TABLE IF EXISTS organisation;
CREATE TABLE organisation(
    id INTEGER NOT NULL IDENTITY,
    name VARCHAR(255) NOT NULL UNIQUE,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS user;
CREATE TABLE user(
    id INTEGER NOT NULL IDENTITY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    name VARCHAR(255),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS user_organisation;
CREATE TABLE user_organisation(
    userId INTEGER NOT NULL REFERENCES user(id),
    organisationId INTEGER NOT NULL REFERENCES organisation(id),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS user_role;
CREATE TABLE user_role(
    userId INTEGER NOT NULL REFERENCES user(id),
    roleId INTEGER NOT NULL REFERENCES role(id),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

// Create some system data
INSERT INTO role(id, name) VALUES(0, 'SUPERUSER');
INSERT INTO role(id, name) VALUES(1, 'MONITOR');
INSERT INTO role(id, name) VALUES(2, 'AGENT');
INSERT INTO role(id, name) VALUES(3, 'USER');
INSERT INTO organisation(id, name) VALUES(0, 'CodeKvast');
INSERT INTO organisation(id, name) VALUES(1, 'Demo');
INSERT INTO user(id, username, password) VALUES(0, 'root', HASH('SHA256', STRINGTOUTF8('Gqb9av78zNTF:root'), 1));
INSERT INTO user(id, username, password) VALUES(1, 'user', HASH('SHA256', STRINGTOUTF8('Gqb9av78zNTF:0000'), 1));
INSERT INTO user(id, username, password) VALUES(2, 'monitor', HASH('SHA256', STRINGTOUTF8('Gqb9av78zNTF:0000'), 1));
INSERT INTO user_organisation(userId, organisationId) VALUES(0, 0);
INSERT INTO user_organisation(userId, organisationId) VALUES(1, 0);
INSERT INTO user_role(userId, roleId) VALUES(0, 0);
INSERT INTO user_role(userId, roleId) VALUES(1, 2);
INSERT INTO user_role(userId, roleId) VALUES(1, 3);
INSERT INTO user_role(userId, roleId) VALUES(2, 1);
