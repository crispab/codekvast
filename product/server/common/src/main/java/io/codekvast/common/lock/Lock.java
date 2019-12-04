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

import lombok.*;

import java.time.Duration;
import java.time.Instant;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class Lock {
    @NonNull
    private final String name;

    private final Long customerId;

    @NonNull
    private final Integer maxLockWaitSeconds;

    private final Instant waitStartedAt = Instant.now();

    @With
    private final Instant acquiredAt;

    @With
    private final Instant releasedAt;

    public String key() {
        return "codekvast-" + customerId == null ? name : name + "-" + customerId;
    }

    public Duration getWaitDuration() {
        return Duration.between(waitStartedAt, acquiredAt);
    }

    public Duration getLockDuration() {
        return Duration.between(acquiredAt, releasedAt);
    }

    @Override
    public String toString() {
        return String.format("%s(key=%s)", Lock.class.getSimpleName(), key());
    }

    public static Lock forFunction(@NonNull String name) {
        return Lock.builder()
                   .name(name)
                   .maxLockWaitSeconds(2)
                   .build();
    }

    public static Lock forSystem() {
        return forFunction("system");
    }

    public static Lock forCustomer(@NonNull Long customerId) {
        return Lock.builder()
                   .name("customer")
                   .customerId(customerId)
                   .maxLockWaitSeconds(120)
                   .build();
    }

}
