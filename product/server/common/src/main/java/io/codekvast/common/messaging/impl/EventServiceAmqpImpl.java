/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
package io.codekvast.common.messaging.impl;

import static io.codekvast.common.messaging.impl.RabbitmqConfig.CODEKVAST_EVENT_QUEUE;

import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.model.CodekvastEvent;
import io.codekvast.common.metrics.CommonMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.stereotype.Service;

/**
 * An AMQP implementation of the EventService.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceAmqpImpl implements EventService {

  private final AmqpTemplate amqpTemplate;
  private final CommonMetricsService metricsService;

  @Override
  public void send(CodekvastEvent event) {
    logger.debug("Sending {} to {}", event, CODEKVAST_EVENT_QUEUE);
    amqpTemplate.convertAndSend(
        CODEKVAST_EVENT_QUEUE,
        event,
        message -> {
          logger.trace("Message={}", message);
          message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          return message;
        });
  }
}
