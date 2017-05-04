package io.codekvast.warehouse.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class PasswordReplacer implements ApplicationListener<ApplicationReadyEvent> {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public PasswordReplacer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void replacePlaintextPasswords() {
        jdbcTemplate.query("SELECT id, name, plaintextPassword FROM customers WHERE plaintextPassword IS NOT NULL",
                           rs -> {
                               long id = rs.getLong(1);
                               String name = rs.getString(2);
                               String plaintextPassword = rs.getString(3);

                               int updated = jdbcTemplate
                                   .update("UPDATE customers SET encodedPassword = ?, plaintextPassword = NULL WHERE id = ?",
                                           passwordEncoder.encode(plaintextPassword), id);
                               if (updated == 1) {
                                   log.info("Encoded plaintext password for customer '{}' (id={})", name, id);
                               } else {
                                   log.warn("Failed to encode plaintext password for customer '{}' (id={})", name, id);
                               }
                           }
        );
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        replacePlaintextPasswords();
    }
}
