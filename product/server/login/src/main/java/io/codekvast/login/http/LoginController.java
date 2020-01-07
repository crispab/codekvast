/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.login.http;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.security.CipherException;
import io.codekvast.login.heroku.HerokuService;
import io.codekvast.login.metrics.LoginMetricsService;
import io.codekvast.login.model.User;
import io.codekvast.login.service.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final HerokuService herokuService;
    private final LoginMetricsService metricsService;

    @GetMapping("/userinfo")
    public String userinfo(OAuth2AuthenticationToken authentication, Model model) {
        User user = loginService.getUserFromAuthentication(authentication);
        Set<String> roles = authentication.getAuthorities()
                                          .stream()
                                          .map(GrantedAuthority::getAuthority)
                                          .filter(a -> a.startsWith("ROLE_"))
                                          .collect(Collectors.toSet());
        logger.debug("roles={}", roles);

        model.addAttribute("title", "Projects");
        model.addAttribute("hasErrorMessage", user.getErrorMessage() != null);
        model.addAttribute("errorMessage", user.getErrorMessage());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("hasNoProject", user.getErrorMessage() == null && user.getCustomerData().size() == 0);
        model.addAttribute("hasProject", user.getCustomerData().size() > 0);
        model.addAttribute("projects", user.getCustomerData().stream().map(ProjectInfo::new).collect(Collectors.toList()));
        model.addAttribute("isAdmin", roles.contains("ROLE_ADMIN"));

        logger.trace("Model={}", model);
        return "userinfo";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("title", "Login");
        return "login";
    }

    @GetMapping("/admin/heroku")
    public String herokuCustomers(OAuth2AuthenticationToken authentication, Model model) {

        User user = loginService.getUserFromAuthentication(authentication);
        model.addAttribute("title", "Tokens");
        model.addAttribute("email", user.getEmail());
        model.addAttribute("customers", customerService.getCustomerData().stream()
                                                       .filter(customerData -> customerData.getSource()
                                                                                           .equals(CustomerService.Source.HEROKU))
                                                       .collect(Collectors.toList()));
        logger.trace("Model={}", model);
        return "herokuCustomers";
    }

    @GetMapping("/admin/heroku/{customerId}")
    public String herokuDetails(OAuth2AuthenticationToken authentication, Model model, @PathVariable("customerId") Long customerId)
        throws CipherException {
        User user = loginService.getUserFromAuthentication(authentication);
        model.addAttribute("title", "Tokens");
        model.addAttribute("email", user.getEmail());
        model.addAttribute("customerName", customerService.getCustomerDataByCustomerId(customerId).getCustomerName());
        model.addAttribute("callbackUrl", herokuService.getCallbackUrlFor(customerId));
        model.addAttribute("accessToken", herokuService.getAccessTokenFor(customerId));
        Instant expiresAt = herokuService.getAccessTokenExpiresAtFor(customerId);
        if (expiresAt != null) {
            model.addAttribute("expiresAt", expiresAt);
            model.addAttribute("expiresIn", Duration.between(Instant.now(), expiresAt));
        }
        logger.trace("Model={}", model);
        return "herokuDetails";
    }

    @GetMapping({"/", "/index", "/home"})
    public String index(HttpServletRequest request, Authentication authentication, Model model) {
        logger.debug("index(): Request.contextPath={}", request.getContextPath());
        model.addAttribute("title", "");
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

    @Value
    private static class ProjectInfo {
        Long customerId;
        String displayName;
        Instant createdAt;
        Instant collectionStartedAt;
        String collectionStartedAtClass;
        String comment;
        String commentClass;

        private ProjectInfo(CustomerData cd) {
            this.customerId = cd.getCustomerId();
            this.displayName = cd.getDisplayName();
            this.createdAt = cd.getCreatedAt();
            this.collectionStartedAt = cd.getCollectionStartedAt();
            this.collectionStartedAtClass = cd.getCollectionStartedAt() == null ? "" : "table-success";

            List<String> comments = new ArrayList<>();
            String commentClass = "";

            if (this.collectionStartedAt == null) {
                comments.add("No data has yet been collected.");
                commentClass = "table-warning";
            } else if (cd.isTrialPeriodExpired(Instant.now())) {
                comments.add("Trial period expired at " + cd.getTrialPeriodEndsAt() + ".");
                commentClass = "table-danger";
            } else if (cd.getTrialPeriodEndsAt() != null) {
                comments.add("Trial period ends at " + cd.getTrialPeriodEndsAt() + ".");
                commentClass = "table-light";
            }
            this.comment = comments.stream().collect(Collectors.joining("<br>"));
            this.commentClass = commentClass;
        }
    }
}
