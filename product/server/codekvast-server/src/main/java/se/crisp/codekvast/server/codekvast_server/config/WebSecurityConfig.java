package se.crisp.codekvast.server.codekvast_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import se.crisp.codekvast.server.daemon_api.AgentRestEndpoints;
import se.crisp.codekvast.server.codekvast_server.model.Role;

import javax.inject.Inject;
import javax.sql.DataSource;

/**
 * Configures web security.
 *
 * @author olle.hallin@crisp.se
 */
@Configuration
@EnableWebSecurity
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 30 * 60)
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * @param passwordEncoder The password encoder to use for encoding plaintext passwords received in login requests.
     * @param dataSource      The dataSource to use.
     * @param auth            The object to configure.
     * @throws Exception
     */
    @Inject
    public void configureGlobal(PasswordEncoder passwordEncoder, DataSource dataSource, AuthenticationManagerBuilder auth)
            throws Exception {
        // @formatter:off
        auth.jdbcAuthentication().dataSource(dataSource)
            .usersByUsernameQuery("SELECT username, encoded_password, enabled FROM users WHERE username = ?")

            .authoritiesByUsernameQuery("SELECT users.username, user_roles.role FROM users, user_roles " +
                                        "WHERE users.id = user_roles.user_id AND users.username = ?")
            .rolePrefix(Role.ANNOTATION_PREFIX)
            .passwordEncoder(passwordEncoder);
        // @formatter:on
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http.csrf().disable()
            .authorizeRequests()
                .antMatchers("/webjars/**").permitAll()
                .antMatchers("/app/**").permitAll()
                .antMatchers("/register/**").permitAll()
                .antMatchers("/management/**").hasRole(Role.MONITOR.name())
                .antMatchers(AgentRestEndpoints.PREFIX + "**").hasRole(Role.AGENT.name())
                .antMatchers("/**").hasRole(Role.USER.name())
                .and()
            // an interactive user uses form login
            .formLogin()
                .loginPage("/login").permitAll()
                .and()
            // The agent uses BASIC authentication
            .httpBasic()
                .realmName("Codekvast")
                .and()
            .logout()
                .permitAll();
        // @formatter:on
    }
}
