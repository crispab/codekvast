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
     * @param passwordEncoder The password encoder to use for matching login requests.
     * @param dataSource      The dataSource to use.
     * @param auth            The object to configure.
     * @throws Exception
     */
    @Inject
    public void configureGlobal(PasswordEncoder passwordEncoder, DataSource dataSource, AuthenticationManagerBuilder auth)
            throws Exception {
        auth.jdbcAuthentication().dataSource(dataSource)
            .usersByUsernameQuery("SELECT username, password, enabled FROM users WHERE username = ?")
            .authoritiesByUsernameQuery("SELECT users.username, users_roles.role FROM users_roles, users " +
                                                " WHERE users_roles.userId = users.id AND users.username = ?")
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
