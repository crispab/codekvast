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
package io.codekvast.backoffice.metrics.impl

import io.codekvast.backoffice.metrics.BackofficeMetricsService
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class BackofficeMetricsServiceImpl(private val meterRegistry: MeterRegistry) : BackofficeMetricsService {

    val logger = LoggerFactory.getLogger(javaClass)!!

    override fun recordEventProcessingTime(duration: Duration) {
        val timer = meterRegistry.timer("codekvast.backoffice.event_processed_in.millis")

        timer.record(duration)

        val count = timer.count()
        if (count % 1000 == 0L) {
            logger.info("Received {} events. Processing time max = {} ms, mean = {} ms",
                count,
                formatDouble(timer.max(TimeUnit.MILLISECONDS)),
                formatDouble(timer.mean(TimeUnit.MILLISECONDS)))
        }
    }

    fun formatDouble(d: Double) = "%.1f".format(Locale.ENGLISH, d)
}
