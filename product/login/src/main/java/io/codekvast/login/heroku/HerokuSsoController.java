/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.login.heroku;

import io.codekvast.common.security.SecurityService;
import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * MVC endpoint which takes care of 'heroku addons:open codekvast'
 *
 * @author olle.hallin@crisp.se
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class HerokuSsoController {

    private final CodekvastLoginSettings settings;
    private final SecurityService securityService;

    @ExceptionHandler
    public ResponseEntity<String> onAuthenticationException(AuthenticationException e) {
        logger.warn("Invalid SSO attempt");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @RequestMapping(path = "/heroku/sso/", method = POST, consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public void doHerokuSingleSignOn(
        @RequestParam("id") String externalId,
        @RequestParam("timestamp") long timestampSeconds,
        @RequestParam("token") String token,
        @RequestParam("nav-data") String navData,
        @RequestParam("email") String email,
        HttpServletResponse response) throws AuthenticationException {

        logger.debug("externalId={}, nav-data={}", externalId, navData);

        String code = securityService.doHerokuSingleSignOn(token, externalId, email, timestampSeconds);

        response.setStatus(HttpStatus.TEMPORARY_REDIRECT.value());
        response
            .setHeader(HttpHeaders.LOCATION, String.format("%s/dashboard/heroku/sso/%s/%s", settings.getDashboardBaseUrl(), code, navData));
    }

}
