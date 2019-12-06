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

import io.codekvast.common.lock.Lock;
import io.codekvast.common.lock.LockManager;
import io.codekvast.common.metrics.CommonMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LockManagerImpl implements LockManager {

    private final JdbcTemplate jdbcTemplate;
    private final CommonMetricsService metricsService;
    private final Clock clock;

    @Override
    public Optional<Lock> acquireLock(final Lock lock) {
        Integer locked = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, ?)", Integer.class, lock.key(), lock.getMaxLockWaitSeconds());
        if (locked.equals(1)) {
            Lock result = lock.withAcquiredAt(clock.instant());
            logger.debug("Acquired lock {}", result);
            return Optional.of(result);
        }
        logger.info("Failed to acquire lock {}", lock);
        metricsService.countLockFailure(lock);
        return Optional.empty();
    }

    @Override
    public void releaseLock(Lock lock) {
        Integer unlocked = jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, lock.key());
        if (unlocked.equals(1)) {
            logger.debug("Released lock {}", lock);
            metricsService.recordLockUsage(lock.withReleasedAt(clock.instant()));
        } else {
            logger.warn("Attempt to release lock {} which was not previously acquired", lock);
        }
    }
}
