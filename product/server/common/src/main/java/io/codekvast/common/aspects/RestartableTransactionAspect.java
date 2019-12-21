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
package io.codekvast.common.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;

import static io.codekvast.common.util.LoggingUtils.humanReadableDuration;

/**
 * A handler for @Restartable methods that encounters an exception that indicates that the transaction has encountered a deadlock or
 * lock wait timeout.
 *
 * It will retry the method a number of times with a short random delay.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE) // Make it wrap @Transactional
@Slf4j
public class RestartableTransactionAspect {

    private final Random random = new Random();

    @Pointcut("execution(* *(..))")
    private void methodExecution() {}

    @Pointcut("within(io.codekvast..*)")
    private void withinCodekvast() {}

    @Pointcut("@annotation(io.codekvast.common.aspects.Restartable)")
    private void anyRestartable() {}

    @Around("anyRestartable() && methodExecution() && withinCodekvast()")
    public Object restartableOperation(ProceedingJoinPoint pjp) throws Throwable {
        String joinPoint = pjp.toShortString();
        logger.debug("Before {}", joinPoint);
        final int maxAttempt = 3;
        for (int attempt = 1; attempt < maxAttempt; attempt++) {
            Instant startedAt = Instant.now();
            try {
                Object result = pjp.proceed();
                logger.debug("After {}", joinPoint);
                return result;
            } catch (Throwable t) {
                if (isRetryableException(t)) {
                    int delayMillis = getRandomInt(10, 50);
                    logger.info("Deadlock #{} after {} in {}, will retry in {} ms. Cause={}.", attempt, humanReadableDuration(startedAt, Instant.now()), joinPoint, delayMillis, getCause(t));
                    Thread.sleep(delayMillis);
                } else {
                    throw t;
                }
            }
        }
        logger.warn("Executing a last retry attempt at {}", joinPoint);
        return pjp.proceed();
    }

    private String getCause(Throwable t) {
        if (t.getCause() == null) {
            return t.toString();
        }
        return getCause(t.getCause());
    }

    boolean isRetryableException(Throwable t) {
        if (t == null) {
            return false;
        }
        String s = t.toString().toLowerCase();
        if (s.contains("deadlock") || s.contains("lock wait timeout")) {
            return true;
        }
        return isRetryableException(t.getCause());
    }

    private int getRandomInt(int min, int max) {
        return min + random.nextInt(max - min);
    }
}
