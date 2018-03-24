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
package io.codekvast.login.bootstrap;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.login.model.Roles;
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
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED;

/**
 * @author olle.hallin@crisp.se
 */
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
        //@formatter:off
        http
            .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringAntMatchers("/heroku/**", "/launch/**", LOGOUT_URL)
            .and()
                .sessionManagement().sessionCreationPolicy(IF_REQUIRED)
            .and()
                .logout()
                    .logoutUrl(LOGOUT_URL)
                    .logoutSuccessUrl("/")
            .and()
                .authorizeRequests()
                // .antMatchers("/", "/home", "/login", "/api/isAuthenticated", "/api/dashboard/baseUrl", "/heroku/**").permitAll()
                    .antMatchers("/favicon.ico", "/robots.txt", "/assets/**", LOGIN_URL).permitAll()
                    .anyRequest().authenticated()
            .and()
                .oauth2Login()
                    .userInfoEndpoint().userAuthoritiesMapper(userAuthoritiesMapper())
                .and()
                    .loginPage(LOGIN_URL)
                    .authorizationEndpoint().baseUri(OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);

        //@formatter:on
    }

    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                mappedAuthorities.add(authority);

                Object email = null;
                if (OidcUserAuthority.class.isInstance(authority)) {
                    email = ((OidcUserAuthority)authority).getUserInfo().getClaims().get("email");
                } else if (OAuth2UserAuthority.class.isInstance(authority)) {
                    email = ((OAuth2UserAuthority)authority).getAttributes().get("email");
                }
                if (email != null) {
                    List<CustomerData> customerData = customerService.getCustomerDataByUserEmail(email.toString());
                    if (customerData != null && !customerData.isEmpty()) {
                        mappedAuthorities.add(new SimpleGrantedAuthority(Roles.CUSTOMER));
                    }
                }
            });
            return mappedAuthorities;
        };
    }
}
