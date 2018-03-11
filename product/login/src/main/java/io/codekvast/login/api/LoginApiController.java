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
package io.codekvast.login.api;

import com.google.common.net.HttpHeaders;
import io.codekvast.login.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Implements the API used by login-api.service.ts
 */
@RestController
@CrossOrigin(origins = "http://localhost:8088")
@RequiredArgsConstructor
@Slf4j
public class LoginApiController {

    private final LoginService loginService;

    /**
     * This is an unprotected endpoint that returns true if the user is authenticated.
     *
     * @param principal The logged in principal, or null if unauthenticated.
     * @return true iff the user is authenticated.
     */
    @RequestMapping("/api/isAuthenticated")
    public boolean isAuthenticated(Principal principal) {
        logger.debug("isAuthenticated(): {}", principal != null);
        return principal != null;
    }

    /**
     * This is a protected endpoint that requires authentication.
     *
     * @param authentication The OAuth2 authentication object
     * @return A User object.
     */
    @RequestMapping("/api/user")
    public User user(Authentication authentication) {
        logger.info("Authentication={}", authentication);

        return loginService.getUserFromAuthentication(authentication);
    }

    @RequestMapping(path = "/api/launchDashboard/{customerId}", method = POST)
    public ResponseEntity<String> launchDashboard(@PathVariable("customerId") Long customerId,
                                                  Principal principal,
                                                  HttpServletResponse response) {
        String link = loginService.getDashboardLaunchLink(customerId);

        if (link != null) {
            logger.info("{} is launching dashboard for customerId {}", principal.getName(), customerId);
            return ResponseEntity.status(HttpStatus.FOUND.value()).header(HttpHeaders.LOCATION, link).body(link);
        }

        logger.warn("{} has no rights to launch dashboard for customerId {}", principal.getName(), customerId);
        return ResponseEntity.notFound().build();
    }

}
