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
package io.codekvast.common.lock;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** @author olle.hallin@crisp.se */
@RequiredArgsConstructor
@Component
public class LockTemplate {
  private final LockManager lockManager;

  /**
   * Acquires a lock, executes an action, and then releases the lock.
   *
   * @param lock The lock to acquire
   * @param lockAction The action to execute within the lock
   * @param failedAction The value to return if failure to acquire the lock.
   * @param <V> The type to return
   * @return The result of either lockAction or failedAction, depending on if the lock was acquired.
   */
  public <V> V doWithLock(Lock lock, Callable<V> lockAction, Supplier<V> failedAction)
      throws Exception {
    Optional<Lock> optionalLock = lockManager.acquireLock(lock);
    if (optionalLock.isPresent()) {
      try {
        return lockAction.call();
      } finally {
        lockManager.releaseLock(optionalLock.get());
      }
    }
    return failedAction.get();
  }

  /**
   * Executes an action within a lock.
   *
   * @param lock The lock to acquire
   * @param runnable The action to perform within the lock.
   */
  public void doWithLock(Lock lock, Runnable runnable) {
    Optional<Lock> optionalLock = lockManager.acquireLock(lock);
    if (optionalLock.isPresent()) {
      try {
        runnable.run();
      } finally {
        lockManager.releaseLock(optionalLock.get());
      }
    }
  }
}
