/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
package io.codekvast.dashboard.bootstrap;

import static org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED;

import io.codekvast.common.security.SecurityService;
import io.codekvast.dashboard.dashboard.DashboardTokenFilter;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Component;

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
@Slf4j
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  private final UnauthorizedHandler unauthorizedHandler;
  private final DashboardTokenFilter dashboardTokenFilter;

  @Override
  protected void configure(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        .csrf()
        .ignoringAntMatchers(
            "/management/**", "/javaagent/**", "/dashboard/launch/**", "/dashboard/heroku/sso/**")
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .and()
        .sessionManagement()
        .sessionCreationPolicy(IF_REQUIRED)
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(unauthorizedHandler)
        .and()
        .authorizeRequests()
        .antMatchers(
            "/management/**",
            "/dashboard/launch/**",
            "/dashboard/heroku/sso/**",
            "/dashboard/api/v1/serverSettings",
            "/javaagent/**")
        .permitAll()
        .antMatchers("/dashboard/**")
        .hasRole(SecurityService.USER_ROLE)
        .and()
        .addFilterBefore(dashboardTokenFilter, UsernamePasswordAuthenticationFilter.class)
        .headers()
        .cacheControl();
  }

  @Component
  public static class UnauthorizedHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException)
        throws IOException {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
  }
}
