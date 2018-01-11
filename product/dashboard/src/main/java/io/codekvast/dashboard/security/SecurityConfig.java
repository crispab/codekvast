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

import io.codekvast.dashboard.bootstrap.CodekvastSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
    private final SecurityService securityService;
    private final CodekvastSettings settings;

    @Bean
    public AuthenticationTokenFilter authenticationTokenFilter() {
        return new AuthenticationTokenFilter();
    }

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
                .addFilterBefore(authenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);

            // disable page caching
            httpSecurity.headers().cacheControl();
        }
    }

    @Component
    public static class UnauthorizedHandler implements AuthenticationEntryPoint {

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        }
    }

    private class AuthenticationTokenFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

            securityService.authenticateToken(getSessionToken(request));
            try {
                chain.doFilter(request, response);
            } finally {
                securityService.removeAuthentication();
            }
        }

        private String getSessionToken(HttpServletRequest request) {
            String token = getTokenFromCookie(request, SESSION_TOKEN_COOKIE);
            if (token != null) {
                logger.debug("Found sessionToken in cookie");
            } else  {
                token = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (token != null) {
                    logger.debug("Found sessionToken in header");
                }
            }
            return token;
        }

        private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(cookieName)) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        }

    }
}