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
package io.codekvast.login.bootstrap;

import static org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.security.Roles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/** @author olle.hallin@crisp.se */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private static final String LOGOUT_URL = "/logout";
  private static final String LOGIN_URL = "/login";

  private final CustomerService customerService;

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    http.csrf()
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .ignoringAntMatchers("/heroku/**", "/management/**", "/launch/**", LOGOUT_URL)
        .and()
        .sessionManagement()
        .sessionCreationPolicy(IF_REQUIRED)
        .and()
        .logout()
        .logoutUrl(LOGOUT_URL)
        .logoutSuccessUrl("/")
        .and()
        .authorizeRequests()
        .antMatchers(
            "/favicon.ico", "/robots.txt", "/management/**", "/assets/**", "/heroku/**", LOGIN_URL)
        .permitAll()
        .antMatchers("/admin/**")
        .hasRole("ADMIN")
        .anyRequest()
        .authenticated()
        .and()
        .oauth2Login()
        .userInfoEndpoint()
        .userAuthoritiesMapper(userAuthoritiesMapper())
        .and()
        .loginPage(LOGIN_URL)
        .authorizationEndpoint()
        .baseUri(OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
  }

  private GrantedAuthoritiesMapper userAuthoritiesMapper() {
    return (authorities) -> {
      Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

      authorities.forEach(
          authority -> {
            grantedAuthorities.add(authority);

            Object email = null;

            //noinspection ChainOfInstanceofChecks
            if (authority instanceof OAuth2UserAuthority) {
              email = ((OAuth2UserAuthority) authority).getAttributes().get("email");
            } else if (authority instanceof SimpleGrantedAuthority) {
              logger.debug("Ignoring extra authority {}", authority);
            } else {
              logger.debug(
                  "Don't know how to extract the email address from a {}",
                  authority.getClass().getName());
            }

            if (email != null) {
              List<CustomerData> customerData =
                  customerService.getCustomerDataByUserEmail(email.toString());
              if (!customerData.isEmpty()) {
                grantedAuthorities.add(new SimpleGrantedAuthority(Roles.CUSTOMER));
              }

              List<String> roleNames = customerService.getRoleNamesByUserEmail(email.toString());
              for (String roleName : roleNames) {
                grantedAuthorities.add(new SimpleGrantedAuthority(roleName));
              }
            }
          });
      return grantedAuthorities;
    };
  }
}
