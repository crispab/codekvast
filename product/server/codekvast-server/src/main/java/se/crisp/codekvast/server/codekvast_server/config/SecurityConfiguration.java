package se.crisp.codekvast.server.codekvast_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.inject.Inject;
import javax.sql.DataSource;

/**
 * @author Olle Hallin
 */
@Configuration
// @EnableWebMvcSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Inject
    public void configureGlobal(DataSource dataSource, PasswordEncoder passwordEncoder, AuthenticationManagerBuilder auth)
            throws Exception {
        // auth.inMemoryAuthentication().withUser("user").password("0000").roles("USER");

        auth.jdbcAuthentication().dataSource(dataSource)
            .usersByUsernameQuery("SELECT username, password, enabled FROM users WHERE username = ?")
            .authoritiesByUsernameQuery("SELECT users.username, users_roles.role FROM users_roles, users " +
                                                " WHERE users_roles.userId = users.id AND users.username = ?")
            .passwordEncoder(passwordEncoder);
    }

    /*
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/", "/home").permitAll()
                .anyRequest().authenticated();

        http
                .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
                .logout()
                .permitAll();
    }
    */
}
