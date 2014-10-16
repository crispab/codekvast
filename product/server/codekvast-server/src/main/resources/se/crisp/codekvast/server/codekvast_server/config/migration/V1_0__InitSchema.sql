DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
  name VARCHAR(30) NOT NULL UNIQUE,
);

DROP TABLE IF EXISTS organisations;
CREATE TABLE organisations (
    id INTEGER NOT NULL IDENTITY,
    name VARCHAR(255) NOT NULL UNIQUE,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id INTEGER NOT NULL IDENTITY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
  enabled BOOLEAN DEFAULT TRUE NOT NULL,
    email VARCHAR(255),
    name VARCHAR(255),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS users_organisations;
CREATE TABLE users_organisations (
  userId         INTEGER NOT NULL REFERENCES users (id),
  organisationId INTEGER NOT NULL REFERENCES organisations (id),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP INDEX IF EXISTS ix_users_organisations;
CREATE UNIQUE INDEX ix_users_organisations ON users_organisations (userId, organisationId);

DROP TABLE IF EXISTS users_roles;
CREATE TABLE users_roles (
  userId INTEGER     NOT NULL REFERENCES users (id),
  role   VARCHAR(30) NOT NULL REFERENCES roles (name),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

DROP INDEX IF EXISTS ix_users_roles;
CREATE UNIQUE INDEX ix_users_roles ON users_roles (userId, role);

// Create some system data
INSERT INTO roles (name) VALUES ('SUPERUSER');
INSERT INTO roles (name) VALUES ('MONITOR');
INSERT INTO roles (name) VALUES ('AGENT');
INSERT INTO roles (name) VALUES ('USER');
INSERT INTO organisations (id, name) VALUES (0, 'CodeKvast');
INSERT INTO organisations (id, name) VALUES (1, 'Demo');
// The passwords will be hashed by the Java migration V1_1__hashSystemPassword which uses the same PasswordEncoder as Spring Security
INSERT INTO users (id, username, password) VALUES (0, 'root', 'root');
INSERT INTO users (id, username, password) VALUES (1, 'user', '0000');
INSERT INTO users (id, username, password) VALUES (2, 'monitor', '0000');
INSERT INTO users_organisations (userId, organisationId) VALUES (0, 0);
INSERT INTO users_organisations (userId, organisationId) VALUES (1, 0);
INSERT INTO users_roles (userId, role) VALUES (0, 'SUPERUSER');
INSERT INTO users_roles (userId, role) VALUES (1, 'AGENT');
INSERT INTO users_roles (userId, role) VALUES (1, 'USER');
INSERT INTO users_roles (userId, role) VALUES (2, 'MONITOR');
