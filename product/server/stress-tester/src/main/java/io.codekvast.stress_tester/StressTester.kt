/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
package io.codekvast.stress_tester

import io.codekvast.common.messaging.EventService
import io.codekvast.common.messaging.model.AgentPolledEvent
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * @author olle.hallin@crisp.se
 */
@Component
class StressTester(private val eventService: EventService) {

  val logger = LoggerFactory.getLogger(javaClass)!!
  var count = 0
  var firstTime = true
  var startedAt = Instant.now()

  @Scheduled(fixedRateString = "\${codekvast.stress-tester.eventRateMillis}")
  fun sendSampleAgentPolledEvent() {
    val oldName = Thread.currentThread().name
    try {
      Thread.currentThread().name = "Codekvast StressTester"

      if (firstTime) {
        logger.info("StressTester started")
        firstTime = false
      }

      eventService.send(AgentPolledEvent.sample())

      if (++count % 100 == 0) {
        logger.info("Sent {} events {} after start", count, Duration.between(startedAt, Instant.now()).truncatedTo(ChronoUnit.SECONDS))
      }
    } finally {
      Thread.currentThread().name = oldName
    }
  }
}
