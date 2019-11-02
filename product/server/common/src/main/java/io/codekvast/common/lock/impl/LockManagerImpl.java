/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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
package io.codekvast.common.lock.impl;

import io.codekvast.common.lock.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LockManagerImpl implements LockManager {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void populateLocksTable() {
        for (Lock lock : Lock.values()) {
            int inserted = jdbcTemplate.update("INSERT IGNORE INTO internal_locks(name) VALUES(?)", lock.name());
            if (inserted > 0) {
                logger.info("Inserted {} to internal_locks table", lock.name());
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Lock> acquireLock(Lock lock) {
        try {
            String s =
                jdbcTemplate.queryForObject("SELECT name FROM internal_locks WHERE name = ? FOR UPDATE NOWAIT", String.class, lock.name());
            logger.debug("Acquired lock {}", lock);
            return Optional.of(lock);
        } catch (DataAccessException e) {
            logger.info("Failed to acquire lock {}", lock);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseLock(Lock lock) {
        logger.debug("Releasing lock {}", lock);
        // No-op, the lock is automatically released when the transaction ends.
    }
}