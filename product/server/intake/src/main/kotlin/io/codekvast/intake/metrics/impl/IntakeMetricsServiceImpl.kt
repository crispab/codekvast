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
package io.codekvast.intake.metrics.impl

import io.codekvast.intake.metrics.IntakeMetricsService
import io.codekvast.intake.metrics.IntakeStatistics
import io.codekvast.intake.model.PublicationType
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.annotation.PostConstruct

/** @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
class IntakeMetricsServiceImpl(private val meterRegistry: MeterRegistry) : IntakeMetricsService {
    private val queueLengthGauge: AtomicInteger = AtomicInteger(0)
    private val disabledAgentsGauge: AtomicInteger = AtomicInteger(0)
    private val deadAgentsGauge: AtomicInteger = AtomicInteger(0)
    private val aliveAgentsGauge: AtomicInteger = AtomicInteger(0)
    private val publicationLogicalSizeGauges: MutableMap<PublicationType, AtomicInteger> =
        EnumMap(PublicationType::class.java)
    private val publicationPhysicalSizeGauges: MutableMap<PublicationType, AtomicLong> =
        EnumMap(PublicationType::class.java)
    private val ignoredSyntheticSignaturesGauges: MutableMap<PublicationType, AtomicInteger> =
        EnumMap(PublicationType::class.java)

    @PostConstruct
    fun createGauges() {
        meterRegistry.gauge("codekvast.publication.queueLength", queueLengthGauge)

        val agentsName = "codekvast.agents"
        meterRegistry.gauge(agentsName, Tags.of("state", "disabled"), disabledAgentsGauge)
        meterRegistry.gauge(agentsName, Tags.of("state", "dead"), deadAgentsGauge)
        meterRegistry.gauge(agentsName, Tags.of("state", "alive"), aliveAgentsGauge)

        for (type in PublicationType.values()) {
            val tags = getTags(type)
            val logicalSize = AtomicInteger(0)
            publicationLogicalSizeGauges[type] = logicalSize
            meterRegistry.gauge("codekvast.publication.size.entries", tags, logicalSize)
            val physicalSize = AtomicLong(0L)
            publicationPhysicalSizeGauges[type] = physicalSize
            meterRegistry.gauge("codekvast.publication.size.bytes", tags, physicalSize)
            val ignoredSynthetic = AtomicInteger(0)
            ignoredSyntheticSignaturesGauges[type] = ignoredSynthetic
            meterRegistry.gauge("codekvast.publication.synthetic", tags, ignoredSynthetic)
        }
    }

    override fun gaugeAgents(statistics: IntakeStatistics) {
        disabledAgentsGauge.set(statistics.numDisabled)
        deadAgentsGauge.set(statistics.numDead)
        aliveAgentsGauge.set(statistics.numAlive)
    }

    override fun gaugePublicationQueueLength(queueLength: Int) {
        queueLengthGauge.set(queueLength)
    }

    override fun gaugePhysicalPublicationSize(type: PublicationType, sizeInBytes: Long) {
        publicationPhysicalSizeGauges[type]!!.set(sizeInBytes)
    }

    override fun recordImportedPublication(
        type: PublicationType,
        logicalSize: Int,
        ignoredSyntheticSignatures: Int,
        duration: Duration
    ) {
        val tags = getTags(type)
        publicationLogicalSizeGauges[type]!!.set(logicalSize)
        ignoredSyntheticSignaturesGauges[type]!!.set(ignoredSyntheticSignatures)
        meterRegistry.counter("codekvast.publication.accepted", tags).increment()
        meterRegistry.timer("codekvast.publication.import.duration", tags).record(duration)
    }

    override fun countAgentPoll() {
        meterRegistry.counter("codekvast.agent.polls").increment()
    }

    override fun countWeededRows(deletedRows: Int) {
        meterRegistry.counter("codekvast.weeder.deleted_rows").increment(deletedRows.toDouble())
    }

    private fun getTags(type: PublicationType) = Tags.of("type", type.toString())
}