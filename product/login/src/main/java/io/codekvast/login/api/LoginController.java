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

import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.security.CipherException;
import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.heroku.HerokuService;
import io.codekvast.login.metrics.MetricsService;
import io.codekvast.login.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author olle.hallin@crisp.se
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final LoginService loginService;
    private final CustomerService customerService;
    private final CodekvastLoginSettings settings;
    private final HerokuService herokuService;
    private final MetricsService metricsService;

    @ModelAttribute("settings")
    public CodekvastLoginSettings getCodekvastSettings() {
        return settings;
    }

    @ModelAttribute("cookieConsent")
    public Boolean getCookieConsent(@CookieValue(name = "cookieConsent", defaultValue = "FALSE") Boolean cookieConsent) {
        logger.info("cookieConsent={}", cookieConsent);
        return Optional.ofNullable(cookieConsent).orElse(Boolean.FALSE);
    }

    @ModelAttribute("cookieDomain")
    public String cookieDomain(@RequestHeader("Host") String requestHost) {
        logger.info("requestHost={}", requestHost);
        return requestHost.startsWith("localhost") ? "localhost" : ".codekvast.io";
    }

    @GetMapping("/userinfo")
    public String userinfo(OAuth2AuthenticationToken authentication, Model model) {
        User user = loginService.getUserFromAuthentication(authentication);
        logger.info("User = {}", user);
        model.addAttribute("user", user);
        model.addAttribute("roles",
                           authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
        return "userinfo";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/tokens")
    public String tokens(OAuth2AuthenticationToken authentication, Model model) {

        User user = loginService.getUserFromAuthentication(authentication);
        logger.info("User = {}", user);
        model.addAttribute("user", user);
        model.addAttribute("customers", customerService.getCustomerData());
        return "tokens";
    }

    @GetMapping("/tokens/accessToken/{customerId}")
    public String getAccessTokenFor(OAuth2AuthenticationToken authentication, Model model, @PathVariable("customerId") Long customerId)
        throws CipherException {
        User user = loginService.getUserFromAuthentication(authentication);
        logger.info("User = {}", user);
        model.addAttribute("user", user);
        model.addAttribute("customerData", customerService.getCustomerDataByCustomerId(customerId));
        model.addAttribute("callbackUrl", herokuService.getCallbackUrlFor(customerId));
        model.addAttribute("accessToken", herokuService.getAccessTokenFor(customerId));
        return "tokens";
    }

    @GetMapping({"/", "/index", "/home"})
    public String index(HttpServletRequest request, Authentication authentication) {
        logger.debug("index(): Request.contextPath={}", request.getContextPath());
        return authentication == null ? "index" : "redirect:userinfo";
    }

    @PostMapping("/launch/{customerId}")
    public void launchDashboard(@PathVariable("customerId") Long customerId, HttpServletResponse response) {
        User user = loginService.getUserFromSecurityContext();

        URI uri = loginService.getDashboardLaunchURI(customerId);

        if (uri == null) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
        } else {
            logger.debug("Redirecting to {}", uri);
            response.setStatus(HttpStatus.TEMPORARY_REDIRECT.value());
            response.setHeader(HttpHeaders.LOCATION, uri.toString());
            metricsService.countDashboardLaunch();
        }
    }

}
