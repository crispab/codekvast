/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
package io.codekvast.warehouse.messaging.impl;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.webhook.Payload;
import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.messaging.SlackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlackServiceImpl implements SlackService, ApplicationListener<ApplicationReadyEvent> {

    private final CodekvastSettings settings;
    private final Slack slack = Slack.getInstance();

    @Override
    @Async
    public void sendNotification(String text, Channel channel) {
        String ch = channel.name().toLowerCase().replace("_", "-");
        doSendPayload(Payload.builder().text(text).channel(ch).build());
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        sendNotification(
            String.format("%s %s in https://%s has started", settings.getApplicationName(), settings.getDisplayVersion(),
                          settings.getDnsCname()), Channel.BUILDS);
    }

    @PreDestroy
    public void notifyShutdown() {
        sendNotification(String.format("%s %s in https://%s is stopping", settings.getApplicationName(), settings.getDisplayVersion(),
                                       settings.getDnsCname()), Channel.BUILDS);
    }

    private void doSendPayload(Payload payload) {
        String url = getSlackWebhookUrl(settings);
        if (url != null) {
            try {
                long startedAt = System.currentTimeMillis();
                slack.send(url, payload);
                logger.info("Sent '{}' to Slack in {} ms", payload.getText(), System.currentTimeMillis() - startedAt);
            } catch (IOException e) {
                logger.error("Could not send {} to Slack: {}", payload, getRootCause(e));
            }
        } else {
            logger.debug("Would have sent {} to Slack", payload);
        }
    }

    private Throwable getRootCause(Throwable t) {
        return t.getCause() == null ? t : getRootCause(t.getCause());
    }

    private String getSlackWebhookUrl(CodekvastSettings settings) {
        String token = settings.getSlackWebHookToken();
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return String.format("%s/%s", settings.getSlackWebHookUrl(), token);
    }

}
