package se.crisp.codekvast.server.codekvast_server.config.migration;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Replaces plaintext passwords for system users.
 *
 * @author Olle Hallin
 */
@SuppressWarnings("UnusedDeclaration")
@Slf4j
public class V1_1__hashSystemPasswords implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT username, password FROM users");
             PreparedStatement update = connection.prepareStatement("UPDATE users SET password = ? WHERE username = ?")) {

            while (resultSet.next()) {
                String username = resultSet.getString(1);
                String rawPassword = resultSet.getString(2);

                update.setString(1, passwordEncoder.encode(rawPassword));
                update.setString(2, username);
                int updated = update.executeUpdate();
                if (updated == 0) {
                    log.error("Could not encode password for {}", username);
                } else {
                    log.info("Encoded password for {}", username);
                }
            }
        }
    }
}
