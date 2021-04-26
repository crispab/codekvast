/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
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

import java.time.Duration;
import java.time.Instant;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.stereotype.Component;

/**
 * An @Around aspect that wraps the HealthEndpointWebExtension and adds debug logging.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Aspect
@Slf4j
public class HealthCheckLoggingAspect {

  @PostConstruct
  public void aspectLoaded() {
    logger.info("{} loaded", this.getClass().getSimpleName());
  }

  @Around(
      "execution(* org.springframework.boot.actuate.health.HealthEndpointWebExtension.health(org.springframework.boot.actuate.endpoint.http.ApiVersion, org.springframework.boot.actuate.endpoint.SecurityContext, java.lang.String...))")
  public Object logHealthCheckCall(ProceedingJoinPoint pjp) throws Throwable {
    Instant startedAt = Instant.now();
    Object result = null;
    try {
      result = pjp.proceed();
    } catch (Throwable t) {
      logger.debug(
          "Health check failed: {} in {}",
          t.toString(),
          Duration.between(startedAt, Instant.now()));
      throw t;
    } finally {
      if (result instanceof WebEndpointResponse) {
        val rsp = (WebEndpointResponse) result;
        logger.debug(
            "Health check result {} in {}",
            rsp.getStatus(),
            Duration.between(startedAt, Instant.now()));
      } else {
        logger.debug(
            "Health check result {} in {}", result, Duration.between(startedAt, Instant.now()));
      }
    }
    return result;
  }
}
