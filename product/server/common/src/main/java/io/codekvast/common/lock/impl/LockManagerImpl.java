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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.common.lock.impl;

import io.codekvast.common.lock.LockManager;
import io.codekvast.common.metrics.CommonMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LockManagerImpl implements LockManager {

    private final JdbcTemplate jdbcTemplate;
    private final CommonMetricsService metricsService;

    private final ConcurrentHashMap<Lock, Instant> locksAcquiredAt = new ConcurrentHashMap<>();

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
            Instant acquireStartedAt = Instant.now();
            String s =
                jdbcTemplate.queryForObject("SELECT name FROM internal_locks WHERE name = ? FOR UPDATE WAIT ?", String.class, lock.name(), lock.getLockWaitSeconds());
            logger.debug("Acquired lock {}", lock);
            metricsService.recordLockWait(lock, Duration.between(acquireStartedAt, Instant.now()));
            locksAcquiredAt.put(lock, acquireStartedAt);
            return Optional.of(lock);
        } catch (DataAccessException e) {
            logger.info("Failed to acquire lock {} within {} s", lock, lock.getLockWaitSeconds());
            metricsService.countLockFailure(lock);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseLock(Lock lock) {
        logger.debug("Releasing lock {}", lock);
        Instant acquiredAt = locksAcquiredAt.remove(lock);
        if (acquiredAt == null) {
            logger.warn("Attempt to release lock {} which were not previously acquired", lock);
        } else {
            metricsService.recordLockDuration(lock, Duration.between(acquiredAt, Instant.now()));
        }
        // No-op against the database, the lock is automatically released when the transaction ends.
    }
}
