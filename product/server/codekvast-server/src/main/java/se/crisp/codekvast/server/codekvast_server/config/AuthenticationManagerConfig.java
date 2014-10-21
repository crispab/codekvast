package se.crisp.codekvast.server.codekvast_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.inject.Inject;
import javax.sql.DataSource;

/**
 * @author Olle Hallin
 */
@Configuration
@EnableWebSecurity
public class AuthenticationManagerConfig extends WebSecurityConfigurerAdapter {

    /**
     * @param passwordEncoder The password encoder to use for encoding plaintext passwords received in login requests.
     * @param dataSource      The dataSource to use.
     * @param auth            The object to configure.
     * @throws Exception
     */
    @Inject
    public void configureGlobal(PasswordEncoder passwordEncoder, DataSource dataSource, AuthenticationManagerBuilder auth)
            throws Exception {
        auth.jdbcAuthentication().dataSource(dataSource)
            .usersByUsernameQuery("SELECT username, encoded_password, enabled FROM users WHERE username = ?")
            .authoritiesByUsernameQuery("SELECT users.username, user_roles.role FROM users, user_roles " +
                                                " WHERE users.id = user_roles.user_id AND users.username = ?")
            .rolePrefix("ROLE_")
            .passwordEncoder(passwordEncoder);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .httpBasic().realmName("CodeKvast")
            .and().authorizeRequests().antMatchers("/agent/**").hasRole("AGENT").antMatchers("/webui/**").hasRole("USER");

    }

}
