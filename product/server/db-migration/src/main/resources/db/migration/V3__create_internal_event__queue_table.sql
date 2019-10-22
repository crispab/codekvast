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
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
-- THE SOFTWARE.
--

CREATE TABLE internal_event_queue (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    createdAt         TIMESTAMP    NOT NULL,
    eventId           VARCHAR(40)  NOT NULL,
    correlationId     VARCHAR(40)  NOT NULL,
    environment       VARCHAR(30)  NOT NULL,
    sendingApp        VARCHAR(30)  NOT NULL,
    sendingAppVersion VARCHAR(30)  NOT NULL,
    sendingHostname   VARCHAR(80)  NOT NULL,
    type              VARCHAR(255) NOT NULL,
    data              TEXT         NOT NULL
);

CREATE TABLE internal_locks (
    name VARCHAR(80) PRIMARY KEY NOT NULL
);
