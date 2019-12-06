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
package io.codekvast.common.lock;

import java.util.Optional;

/**
 * A low-level lock manager.
 *
 * This class is not intended to be used directly. It should be used from {@link LockTemplate}.
 *
 * @author olle.hallin@crisp.se
 * @see LockTemplate
 */
public interface LockManager {

    /**
     * Acquire a lock.
     *
     * @param lock The lock to acquire.
     * @return A filled optional if the lock was acquired, else an empty optional.
     */
    Optional<Lock> acquireLock(Lock lock);

    /**
     * Releases a lock acquired by {@link #acquireLock(Lock)}.
     * Should be invoked in a finally block.
     *
     * @param lock The lock to release.
     */
    void releaseLock(Lock lock);
}
