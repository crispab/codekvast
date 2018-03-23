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
package io.codekvast.login.api.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.security.SecurityService;
import io.codekvast.common.security.WebappCredentials;
import io.codekvast.login.api.LoginService;
import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final CodekvastLoginSettings settings;
    private final CustomerService customerService;
    private final SecurityService securityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public URI getDashboardLaunchURI(Long customerId) {
        User user = getUserFromSecurityContext();

        if (user.getCustomerData().stream().anyMatch(cd -> cd.getCustomerId().equals(customerId))) {
            customerService.registerLogin(
                CustomerService.LoginRequest.builder()
                                            .customerId(customerId)
                                            .email(user.getEmail())
                                            .source(settings.getApplicationName())
                                            .build());
            CustomerData cd = customerService.getCustomerDataByCustomerId(customerId);

            String code = securityService.createCodeForWebappToken(
                customerId,
                WebappCredentials
                    .builder()
                    .customerName(cd.getCustomerName())
                    .email(user.getEmail())
                    .source(cd.getSource())
                    .build());
            return URI.create(String.format("%s/dashboard/launch/%s", settings.getDashboardBaseUrl(), code));
        }
        return null;
    }

    @Override
    public User getUserFromSecurityContext() {
        return getUserFromAuthentication((OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication());
    }

    @Override
    public User getUserFromAuthentication(OAuth2AuthenticationToken authentication) {
        //noinspection unchecked
        Map<String, Object> details = authentication.getPrincipal().getAttributes();
        logger.debug("Details={}", details);

        String email = (String) details.get("email");

        User user = User.builder()
                        .email(email)
                        .customerData(customerService.getCustomerDataByUserEmail(email))
                        .build();
        logger.debug("Returning {}", user);
        return user;
    }

}
