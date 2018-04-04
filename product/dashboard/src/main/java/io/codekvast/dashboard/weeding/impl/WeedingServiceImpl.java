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
package io.codekvast.dashboard.weeding.impl;

import io.codekvast.dashboard.weeding.WeedingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Service that keeps the database tidy by removing child-less rows in methods, applications and environments.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeedingServiceImpl implements WeedingService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void performDataWeeding() {
        Instant startedAt = Instant.now();
        logger.debug("Performing data weeding");

        int methodCount = jdbcTemplate.update("DELETE m FROM methods AS m\n" +
                                                  "  LEFT JOIN invocations AS i ON m.id = i.methodId\n" +
                                                  "  WHERE i.methodId IS NULL;\n");

        int applicationCount = jdbcTemplate.update("DELETE a FROM applications AS a\n" +
                                                       "  LEFT JOIN invocations AS i ON a.id = i.applicationId\n" +
                                                       "  WHERE i.applicationId IS NULL;\n");

        int environmentCount = jdbcTemplate.update("DELETE e FROM environments AS e\n" +
                                                       "  LEFT JOIN invocations AS i ON e.id = i.environmentId\n" +
                                                       "  WHERE i.environmentId IS NULL;\n");

        if (methodCount + applicationCount + environmentCount > 0) {
            logger.info("Data weeding: {} unreferenced methods, {} empty environments and {} empty applications deleted in {}.",
                        methodCount, environmentCount, applicationCount, Duration.between(startedAt, Instant.now()));
        } else {
            logger.debug("Data weeding: Found nothing to delete");
        }
    }

}
