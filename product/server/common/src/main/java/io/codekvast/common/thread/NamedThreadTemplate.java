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
package io.codekvast.common.thread;

import io.codekvast.common.messaging.CorrelationIdHolder;
import lombok.NonNull;

/**
 * A template for managing thread name and optionally correlationId.
 *
 * @author olle.hallin@crisp.se
 */
public class NamedThreadTemplate {

    /**
     * Executes a task with a certain name on the thread and a new CorrelationId.
     *
     * @param threadName The thread name to set
     * @param task       The task to execute
     */
    public void doInNamedThread(@NonNull String threadName, Runnable task) {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("Codekvast " + threadName);
        CorrelationIdHolder.generateAndSetNew();
        try {
            task.run();
        } finally {
            Thread.currentThread().setName(oldThreadName);
            CorrelationIdHolder.clear();
        }

    }
}
