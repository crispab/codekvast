/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * A health indicator that also is an actuator endpoint.
 *
 * It can be used from a deploy script to make the service return 503 some time before being shut down, so that the load balancer will get a
 * chance of detecting it.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
@Endpoint(id = "lb-health")
public class LoadBalancerHealthIndicator extends AbstractHealthIndicator {

    private volatile Instant outOfServiceSince = null;

    @WriteOperation
    public String outOfService() {
        logger.info("Setting service to out of service");
        this.outOfServiceSince = Instant.now();
        return "Out of service";
    }

    @ReadOperation
    public String state() {
        return outOfServiceSince == null ? "In operation" : "Out of service since " + outOfServiceSince;
    }

    @DeleteOperation
    public String inOperation() {
        logger.info("Setting service to in operation");
        this.outOfServiceSince = null;
        return "In operation";
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if (outOfServiceSince != null) {
            logger.info("Out of service since {}", outOfServiceSince);
            builder.outOfService().withDetail("Out of service since since", outOfServiceSince);
        } else {
            builder.up();
        }
    }
}
