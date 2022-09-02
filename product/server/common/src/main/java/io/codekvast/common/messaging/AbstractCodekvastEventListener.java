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
package io.codekvast.common.messaging;

import static io.codekvast.common.messaging.impl.RabbitmqConfig.CODEKVAST_EVENT_QUEUE;

import io.codekvast.common.messaging.impl.MessageIdRepository;
import io.codekvast.common.messaging.model.CodekvastEvent;
import io.codekvast.common.metrics.CommonMetricsService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.transaction.annotation.Transactional;

/** @author olle.hallin@crisp.se */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCodekvastEventListener {

  private final String consumerName;
  private final MessageIdRepository messageIdRepository;
  private final CommonMetricsService metricsService;
  private final Clock clock;

  @RabbitListener(queues = CODEKVAST_EVENT_QUEUE, ackMode = "AUTO")
  @Transactional(rollbackFor = Exception.class)
  public void onMessage(Message message, @Payload CodekvastEvent event) throws Exception {
    logger.debug("Received {}", message);
    Instant startedAt = clock.instant();
    MessageProperties messageProperties = message.getMessageProperties();
    CorrelationIdHolder.set(messageProperties.getCorrelationId());
    try {
      messageIdRepository.rememberMessageId(messageProperties.getMessageId());
      onCodekvastEvent(event);
    } catch (DuplicateMessageIdException e) {
      logger.warn("Attempt to re-process already processed message {}", message);
    } catch (Exception e) {
      logger.error("Failed to process " + event, e);

      // Move the message to the DLQ for later inspection
      throw e;
    } finally {
      CorrelationIdHolder.clear();
      metricsService.recordEventConsumed(
          consumerName, event, Duration.between(startedAt, clock.instant()));
    }
  }

  /**
   * Business logic for handling one Codekvast event.
   *
   * <p>The event is guaranteed to be unique.
   *
   * @param event The event to handle. Is never null.
   * @throws Exception If the event cannot be handled. Will cause the message to be moved to the
   *     dead letter queue.
   */
  public abstract void onCodekvastEvent(CodekvastEvent event) throws Exception;
}
