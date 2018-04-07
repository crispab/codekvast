--
-- Copyright (c) 2015-2018 Hallin Information Technology AB
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

CREATE TABLE heroku_details (
  id           BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  customerId   BIGINT                NOT NULL UNIQUE,

  callbackUrl  VARCHAR(500)          NOT NULL
  COMMENT 'This is the URL to use for accessing the Heroku Partner API for the associated customer',

  accessToken  TEXT                  NOT NULL
  COMMENT 'Encrypted value of the OAuth access token',

  refreshToken TEXT                  NOT NULL
  COMMENT 'Encrypted value of the OAuth refresh token',

  tokenType    VARCHAR(50)           NOT NULL
  COMMENT 'What type of token is this?',

  expiresAt    TIMESTAMP             NOT NULL
  COMMENT 'When does the access token expire?',

  createdAt    TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT ix_heroku_details_customerId FOREIGN KEY (customerId) REFERENCES customers (id)
);