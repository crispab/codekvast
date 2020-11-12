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
package io.codekvast.common.lock.impl;

import static io.codekvast.common.util.LoggingUtils.humanReadableDuration;

import io.codekvast.common.lock.Lock;
import io.codekvast.common.lock.LockManager;
import io.codekvast.common.metrics.CommonMetricsService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

/** @author olle.hallin@crisp.se */
@Service
@RequiredArgsConstructor
@Slf4j
public class LockManagerImpl implements LockManager {

  private final CommonMetricsService metricsService;
  private final Clock clock;
  private final DataSource dataSource;

  @Override
  public Optional<Lock> acquireLock(Lock lock) {
    Lock acquiredLock = doAcquireLock(lock);
    if (acquiredLock != null) {
      metricsService.recordLockWait(acquiredLock);
      return Optional.of(acquiredLock);
    }
    metricsService.countLockFailure(lock);
    return Optional.empty();
  }

  @Override
  public void releaseLock(Lock lock) {
    val result = doReleaseLock(lock);
    if (result) {
      metricsService.recordLockDuration(lock);
      if (lock.wasLongDuration()) {
        logger.warn(
            "Lock '{}' held for a long time: {} (waited for {})",
            lock.key(),
            humanReadableDuration(lock.getLockDuration()),
            humanReadableDuration(lock.getWaitDuration()));
      }
      logger.trace("Released lock {}", lock);
    } else {
      logger.debug(
          "Attempt to release lock {} which was not previously acquired on this connection", lock);
    }
  }

  private Lock doAcquireLock(Lock lock) {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      boolean locked;
      try (PreparedStatement ps = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
        ps.setString(1, lock.key());
        ps.setInt(2, lock.getMaxLockWaitSeconds());
        try (ResultSet rs = ps.executeQuery()) {
          rs.next();
          locked = rs.getInt(1) == 1;
        }
      }
      if (locked) {
        logger.trace("Acquired lock {}", lock);
        Lock result = lock.withConnection(connection).withAcquiredAt(clock.instant());
        // Prevent the connection from being closed in the finally block, it must remain open until
        // the lock is released.
        connection = null;
        return result;
      }
      if (lock.getMaxLockWaitSeconds() > 0) {
        logger.warn(
            "Failed to acquire lock '{}' within {}s", lock.key(), lock.getMaxLockWaitSeconds());
      } else {
        logger.debug("Task for '{}' is already running", lock.key());
      }
    } catch (SQLException e) {
      logger.warn("Failed to acquire lock " + lock, e);
    } finally {
      doClose(connection);
    }
    return null;
  }

  private boolean doReleaseLock(Lock lock) {
    Connection connection = lock.getConnection();
    try {
      if (connection != null) {
        try (PreparedStatement ps =
            lock.getConnection().prepareStatement("SELECT RELEASE_LOCK(?)")) {
          ps.setString(1, lock.key());
          try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) == 1;
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Failed to release lock " + lock, e);
    } finally {
      doClose(connection);
    }
    return false;
  }

  private void doClose(Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.warn("Could not close connection", e);
      }
    }
  }
}
