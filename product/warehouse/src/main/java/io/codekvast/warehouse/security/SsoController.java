/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
package io.codekvast.warehouse.security;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.customer.CustomerData;
import io.codekvast.warehouse.customer.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.NonceExpiredException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Inject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static javax.xml.bind.DatatypeConverter.printHexBinary;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author olle.hallin@crisp.se
 */
@Controller
@Slf4j
public class SsoController {

    private final CodekvastSettings settings;
    private final MessageDigest sha1;
    private final WebappTokenProvider webappTokenProvider;
    private final CustomerService customerService;

    @Inject
    public SsoController(CodekvastSettings settings, WebappTokenProvider webappTokenProvider, CustomerService customerService)
        throws NoSuchAlgorithmException {
        this.settings = settings;
        this.webappTokenProvider = webappTokenProvider;
        this.customerService = customerService;
        this.sha1 = MessageDigest.getInstance("SHA-1");
    }

    @ExceptionHandler
    public ResponseEntity<String> onAuthenticationException(AuthenticationException e) {
        logger.warn("Invalid SSO attempt");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @RequestMapping(path = "/heroku/sso/", method = POST, consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public String doHerokuSingleSignOn(
        @RequestParam("id") String externalId,
        @RequestParam("timestamp") long timestampSeconds,
        @RequestParam("token") String token,
        @RequestParam("nav-data") String navData,
        @RequestParam("email") String email) throws AuthenticationException {

        logger.debug("externalId={}, nav-data={}", externalId, navData);

        String jwt = doHerokuSingleSignOn(externalId, timestampSeconds, token, email);

        return "redirect:/sso/" + jwt + "/" + navData;
    }

    private String doHerokuSingleSignOn(String externalId, long timestampSeconds, String token, String email)
        throws AuthenticationException {
        String expectedToken = makeHerokuSsoToken(externalId, timestampSeconds);
        logger.debug("id={}, token={}, timestamp={}, expectedToken={}", externalId, token, timestampSeconds, expectedToken);

        long nowSeconds = System.currentTimeMillis() / 1000L;
        if (timestampSeconds > nowSeconds + 60) {
            throw new NonceExpiredException("Timestamp is too far into the future");
        }

        if (timestampSeconds < nowSeconds - 5 * 60) {
            throw new NonceExpiredException("Too old timestamp");
        }

        if (!expectedToken.equals(token)) {
            throw new BadCredentialsException("Invalid token");
        }

        CustomerData customerData = customerService.getCustomerDataByExternalId(externalId);

        customerService.registerLogin(CustomerService.LoginRequest.builder()
                                                                  .customerId(customerData.getCustomerId())
                                                                  .source(customerData.getSource())
                                                                  .email(email)
                                                                  .build());

        return webappTokenProvider.createWebappToken(
            customerData.getCustomerId(),
            WebappCredentials.builder()
                             .externalId(externalId)
                             .customerName(customerData.getCustomerName())
                             .email(email)
                             .source(customerData.getSource())
                             .build());
    }

    String makeHerokuSsoToken(String externalId, long timestampSeconds) {
        return printHexBinary(
            sha1.digest(String.format("%s:%s:%d", externalId, settings.getHerokuApiSsoSalt(), timestampSeconds).getBytes())).toLowerCase();
    }

}
