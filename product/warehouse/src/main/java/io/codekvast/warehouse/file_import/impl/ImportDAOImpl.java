/*
 * Copyright (c) 2015-2017 Crisp AB
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
package io.codekvast.warehouse.file_import.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class ImportDAOImpl implements ImportDAO {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public ImportDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long importApplication(String name, String version, long startedAtMillis) {
        Timestamp createAt = new Timestamp(startedAtMillis);

        int updated = jdbcTemplate.update("UPDATE applications SET createdAt = LEAST(createdAt, ?) " +
                                              "WHERE name = ? AND version = ?", createAt, name, version);
        if (updated != 0) {
            log.debug("Updated application {} {}", name, version);
        } else {
            jdbcTemplate.update("INSERT INTO applications(name, version, createdAt) VALUES (?, ?, ?)",
                                name, version, createAt);
            log.debug("Inserted application {} {} {}", name, version, createAt);
        }

        Long result = jdbcTemplate.queryForObject("SELECT id FROM applications WHERE name = ? AND version = ?", Long.class, name, version);
        log.debug("application.id={}", result);
        return result;
    }
}
