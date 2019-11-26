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

CREATE TABLE synthetic_signature_patterns (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    pattern      VARCHAR(255)                      NOT NULL UNIQUE COMMENT 'Parsed by java.util.regexp.Pattern',
    example      VARCHAR(255)                      NOT NULL COMMENT 'To use in tests',
    errorMessage VARCHAR(255)                      NULL COMMENT 'as returned by java.util.regexp.Pattern.compile()'
);

-- @formatter:off
INSERT INTO synthetic_signature_patterns (pattern, example) VALUES
('.*\\$\\$.*',                            'customer2.controllers.Events..se$crisp$signup4$controllers$Events$$allGuests(se.crisp.signup4.models.Event)'),
('.*\\$\\w+\\$.*',                         'controllers.Assets.play$api$http$HeaderNames$_setter_$LOCATION_$eq(java.lang.String)'),
('.*\\.[A-Z0-9_]+\\(.*\\)$',               'controllers.customer1.application.Admin.CONTENT_MD5()'),
('.*\\$[a-z]+\\(\\)$',                     'controllers.customer1.application.XxxYyy.$amp()'),
('.*\\.\\.anonfun\\..*',                   'controllers.AssetsBuilder..anonfun.at.1..anonfun.apply.2..anonfun.7.apply()'),
('.*\\.\\.EnhancerBySpringCGLIB\\.\\..*',   'customer1.FooConfig..EnhancerBySpringCGLIB..96aac875.CGLIB$BIND_CALLBACKS(java.lang.Object)'),
('.*\\.\\.FastClassBySpringCGLIB\\.\\..*',  'customer1.FooConfig..FastClassBySpringCGLIB..73e1cc5a.getIndex(org.springframework.cglib.core.Signature)'),
('.*\\.canEqual\\(java\\.lang\\.Object\\)', 'io.codekvast.backoffice.facts.CollectionStarted.canEqual(java.lang.Object)');
-- @formatter:on
