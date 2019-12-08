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
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * A handler for @Transactional methods that encounters a {@link DeadlockLoserDataAccessException}.
 *
 * It will retry the transaction a number of times with a short random delay.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Aspect
@Slf4j
public class DeadlockLoserDataAccessExceptionAspect {

    private final Random random = new Random();

    @Around("execution(* io.codekvast..*(..)) && @annotation(io.codekvast.common.aspects.Idempotent)")
    public Object transactionalMethod(ProceedingJoinPoint pjp) throws Throwable {
        String joinPoint = pjp.toShortString();
        logger.trace("Before {}", joinPoint);
        int maxAttempt = 3;
        for (int attempt = 1; attempt < maxAttempt; attempt++) {
            try {
                Object result = pjp.proceed();
                logger.trace("After {}", joinPoint);
                return result;
            } catch (DeadlockLoserDataAccessException e) {
                int delayMillis = getRandomInt(10, 50);
                logger.info("Deadlock #{} at {}, will retry in {} ms ...", attempt, joinPoint, delayMillis);
                Thread.sleep(delayMillis);
            }
        }
        logger.info("Executing a last attempt to retry deadlock at {}", joinPoint);
        return pjp.proceed();
    }

    private int getRandomInt(int min, int max) {
        return min + random.nextInt(max - min);
    }
}
