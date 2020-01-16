--
-- Copyright (c) 2015-2020 Hallin Information Technology AB
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

ALTER TABLE methods
    MODIFY declaringType VARCHAR(256) NULL,
    MODIFY packageName VARCHAR(256) NULL,
    ADD COLUMN annotation VARCHAR(100) NULL AFTER signature;

ALTER TABLE method_locations
    ADD COLUMN annotation VARCHAR(100) NULL AFTER location;

CREATE TABLE packages (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId BIGINT       NOT NULL,
    name       VARCHAR(256) NOT NULL,
    annotation VARCHAR(100) NULL,
    createdAt  TIMESTAMP    NOT NULL,
    CONSTRAINT ix_package_identity UNIQUE (customerId, name),
    CONSTRAINT ix_package_customerId FOREIGN KEY (customerId) REFERENCES customers(id) ON DELETE CASCADE
);

INSERT INTO packages(customerId, name, createdAt)
SELECT customerId, packageName, MIN(createdAt)
    FROM methods
    WHERE packageName IS NOT NULL
    GROUP BY customerId, packageName;

CREATE TABLE types (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId BIGINT       NOT NULL,
    name       VARCHAR(256) NOT NULL,
    annotation VARCHAR(100) NULL,
    createdAt  TIMESTAMP    NOT NULL,
    CONSTRAINT ix_type_identity UNIQUE (customerId, name),
    CONSTRAINT ix_type_customerId FOREIGN KEY (customerId) REFERENCES customers(id) ON DELETE CASCADE
);

INSERT INTO types(customerId, name, createdAt)
SELECT customerId, declaringType, MIN(createdAt)
    FROM methods
    WHERE declaringType IS NOT NULL
    GROUP BY customerId, declaringType;
