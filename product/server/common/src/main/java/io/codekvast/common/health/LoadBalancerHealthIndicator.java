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
package io.codekvast.common.health;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
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
@Endpoint(id = "lbhealth")
public class LoadBalancerHealthIndicator extends AbstractHealthIndicator {

    @SuppressWarnings("WeakerAccess") // Referenced by javadoc
    protected static final String IN_OPERATION = "In operation";

    @SuppressWarnings("WeakerAccess") // Referenced by javadoc
    protected static final String OUT_OF_SERVICE = "Out of service";

    @SuppressWarnings("WeakerAccess") // Referenced by javadoc
    protected static final String OUT_OF_SERVICE_SINCE = OUT_OF_SERVICE + " since ";

    @Nullable
    private volatile Instant outOfServiceSince = null;

    /**
     * Do <pre><code>
     * curl -X POST http:/xxx:$managementPort/management/lbhealth
     * </code> </pre> to set the service to out-of-service,
     * i.e., <code>http://xxx:$managementPort/management/health</code> will return HTTP status 503.
     *
     * @return {@value #OUT_OF_SERVICE_SINCE} yyyy-mm-ddThh:MM:ss.sss
     */
    @WriteOperation
    public String outOfService() {
        logger.info("Setting service to {}", OUT_OF_SERVICE);
        this.outOfServiceSince = Instant.now();
        return OUT_OF_SERVICE_SINCE + outOfServiceSince;
    }

    /**
     * Do <pre><code>
     * curl -X http:/xxx:$managementPort/management/lbhealth to get the current service state.
     * </code></pre>
     *
     * @return {@value #IN_OPERATION} or {@value #OUT_OF_SERVICE_SINCE} yyyy-mm-ddThh:MM:ss.sss
     */
    @ReadOperation
    public String currentState() {
        return outOfServiceSince == null ? IN_OPERATION : OUT_OF_SERVICE_SINCE + outOfServiceSince;
    }

    /**
     * Do <pre><code>
     * curl -X DELETE http:/xxx:$managementPort/management/lbhealth to set the service state to In operation
     * </code></pre>
     *
     * @return {@value #IN_OPERATION}
     */
    @DeleteOperation
    public String inOperation() {
        logger.info("Setting service to {}", IN_OPERATION);
        this.outOfServiceSince = null;
        return IN_OPERATION;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if (outOfServiceSince != null) {
            logger.info("{}{}", OUT_OF_SERVICE_SINCE, outOfServiceSince);
            builder.outOfService().withDetail(OUT_OF_SERVICE_SINCE, outOfServiceSince);
        } else {
            builder.up();
        }
    }
}
