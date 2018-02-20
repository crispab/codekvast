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
package io.codekvast.dashboard.security;

import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Configuration of Spring Security.
 *
 * @author olle.hallin@crisp.se
 */

@SuppressWarnings("SpringJavaAutowiringInspection")
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String REQUEST_MAPPING_WEBAPP_IS_DEMO_MODE = "/webapp/isDemoMode";
    public static final String SESSION_TOKEN_COOKIE = "sessionToken";
    public static final String USER_ROLE = "USER";

    private final UnauthorizedHandler unauthorizedHandler;
    private final CodekvastDashboardSettings settings;
    private final WebappTokenFilter webappTokenFilter;

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            // We cannot use CSRF since agents must be able to POST
            .csrf().disable()

            // and we don't want HttpSessions
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        if (!settings.isDemoMode()) {

            httpSecurity
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()

                .authorizeRequests()

                // /webapp/** should require an authorized user
                .antMatchers(HttpMethod.GET, REQUEST_MAPPING_WEBAPP_IS_DEMO_MODE).permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/webapp/**").permitAll()
                .antMatchers("/webapp/**").hasRole(USER_ROLE)

                // But the rest should be open
                .anyRequest().permitAll();

            // Custom token-based security filter
            httpSecurity
                .addFilterBefore(webappTokenFilter, UsernamePasswordAuthenticationFilter.class);

            // disable page caching
            httpSecurity.headers().cacheControl();
        }
    }

    /**
     * Creates a cookie with the name {@value SESSION_TOKEN_COOKIE}.
     *
     * @param token      The cookie value
     * @param hostHeader The value of the HTTP header "Host". Is used for setting the cookie domain. The port number (if present) is
     *                   ignored.
     * @return A httpOnly session cookie with the path '/'.
     */
    @SneakyThrows(UnsupportedEncodingException.class)
    public Cookie createSessionTokenCookie(String token, String hostHeader) {
        Cookie result = new Cookie(SESSION_TOKEN_COOKIE, URLEncoder.encode(token, "UTF-8"));
        if (hostHeader != null && !hostHeader.isEmpty()) {
            result.setDomain(hostHeader.replaceAll(":[0-9]+$", ""));
        }
        result.setPath("/");
        result.setMaxAge(-1); // Remove when browser exits.
        result.setHttpOnly(true); // Hide from JavaScript in the browser
        return result;
    }

    @Component
    public static class UnauthorizedHandler implements AuthenticationEntryPoint {

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        }
    }

}