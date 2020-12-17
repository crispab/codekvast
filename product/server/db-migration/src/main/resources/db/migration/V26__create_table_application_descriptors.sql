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

CREATE TABLE application_descriptors
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId     BIGINT       NOT NULL,
    applicationId  BIGINT       NOT NULL,
    environmentId  BIGINT       NOT NULL,
    collectedSince TIMESTAMP(3) NOT NULL,
    collectedTo    TIMESTAMP(3) NOT NULL,

    CONSTRAINT ix_application_descriptors_identity UNIQUE (customerId, applicationId, environmentId),
    CONSTRAINT ix_application_descriptors_customerId FOREIGN KEY (customerId) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT ix_application_descriptors_applicationId FOREIGN KEY (applicationId) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT ix_application_descriptors_environmentId FOREIGN KEY (environmentId) REFERENCES environments (id) ON DELETE CASCADE
);

INSERT INTO application_descriptors(customerId, applicationId, environmentId, collectedSince, collectedTo)
SELECT i.customerId, a.id, e.id, GREATEST(a.createdAt, e.createdAt), MAX(i.timestamp)
FROM invocations i
         INNER JOIN applications a ON i.applicationId = a.id
         INNER JOIN environments e ON i.environmentId = e.id
GROUP BY i.customerId, a.id, e.id;
