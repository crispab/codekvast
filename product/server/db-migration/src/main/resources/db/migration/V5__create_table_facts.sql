--
-- Copyright (c) 2015-2019 Hallin Information Technology AB
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

CREATE TABLE facts (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    customerId BIGINT                              NOT NULL,
    type       VARCHAR(75)                         NOT NULL,
    data       VARCHAR(900)                        NOT NULL COMMENT 'JSON representation of a Java object of type type.',
    createdAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updatedAt  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ix_facts_identity UNIQUE (customerId, type, data),
    CONSTRAINT ix_facts_customerId FOREIGN KEY (customerId) REFERENCES customers(id)
);

INSERT IGNORE INTO customers(id, licenseKey, source, name, plan, notes) VALUE (-1, UUID(), 'system', 'no-customer', 'demo',
                                                                               'Owner of facts that are not related to any customer')
