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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.testsupport.docker;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Builder;
import lombok.NonNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

/**
 * A Runnable that can be used as ready-checker when starting a {@link DockerContainer} containing a MariaDB image.
 *
 * @author olle.hallin@crisp.se
 */
@Builder
public class RabbitmqContainerReadyChecker implements ContainerReadyChecker {

    @NonNull
    private final String host;
    private final int internalPort;
    @NonNull
    private final String vhost;
    private int timeoutSeconds;
    private final String username;
    private final String password;
    private final String assignRabbitUrlToSystemProperty;

    @Override
    public int getInternalPort() {
        return internalPort;
    }

    @Override
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void check(int externalPort) throws ContainerNotReadyException {

        String amqpUrl = buildAmqpUrl(externalPort);
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(amqpUrl);
            Connection conn = factory.newConnection();
            conn.close();
        } catch (TimeoutException | IOException e) {
            throw new ContainerNotReadyException(this + " is not ready", e);
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            throw new IllegalArgumentException("Cannot connect to RabbitMQ using " + amqpUrl, e);
        }

        if (assignRabbitUrlToSystemProperty != null) {
            System.setProperty(assignRabbitUrlToSystemProperty, amqpUrl);
        }
    }

    private String buildAmqpUrl(int port) {
        String result = String.format("amqp://%s:%s@%s:%d/%s", username, password, host, port, vhost);
        return result.replaceAll("//$", "");
    }
}
