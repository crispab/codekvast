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
package io.codekvast.testsupport.docker;

import lombok.Builder;
import lombok.NonNull;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A Runnable that can be used as ready-checker when starting a {@link DockerContainer} containing a MariaDB image.
 *
 * @author olle.hallin@crisp.se
 */
@Builder
public class MariaDbContainerReadyChecker implements ContainerReadyChecker {

    @NonNull
    private final String hostname;
    private final int internalPort;
    @NonNull
    private final String database;
    private int timeoutSeconds;
    private final String username;
    private final String password;
    private final String assignJdbcUrlToSystemProperty;

    @Override
    public int getInternalPort() {
        return internalPort;
    }

    @Override
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void check(int externalPort) throws ContainerNotReadyException {
        String jdbcUrl = buildJdbcUrl(externalPort);
        try {
            DataSource dataSource = new MariaDbDataSource(jdbcUrl);

            try (Connection connection = dataSource.getConnection(username, password);
                 Statement st = connection.createStatement()) {

                st.execute("SELECT 1 FROM DUAL");
            }
        } catch (SQLException e) {
            throw new ContainerNotReadyException(this + " is not ready", e);
        }

        if (assignJdbcUrlToSystemProperty != null) {
            System.setProperty(assignJdbcUrlToSystemProperty, jdbcUrl);
        }
    }

    private String buildJdbcUrl(int port) {
        return String.format("jdbc:mariadb://localhost:%d/%s", port, database);
    }
}
