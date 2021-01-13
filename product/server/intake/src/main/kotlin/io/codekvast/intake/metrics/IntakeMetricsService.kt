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
package io.codekvast.intake.metrics

import io.codekvast.intake.model.PublicationType
import java.time.Duration

/**
 * Wrapper for agent metrics.
 *
 * @author olle.hallin@crisp.se
 */
interface IntakeMetricsService {
    /**
     * Updates the gauges for the number of disabled, dead and alive agents.
     *
     * @param statistics The agent statistics.
     */
    fun gaugeAgents(statistics: AgentStatistics)

    /**
     * Updates the gauge for the number of queued publications.
     *
     * @param queueLength The queue length.
     */
    fun gaugePublicationQueueLength(queueLength: Int)

    /**
     * Gauge the size in bytes of a received publication.
     *
     * @param type The type of publication.
     * @param sizeInBytes The physical size in bytes.
     */
    fun gaugePhysicalPublicationSize(type: PublicationType, sizeInBytes: Long)

    /**
     * Record the fact that a publication was imported.
     *
     * @param type The type of publication.
     * @param logicalSize The logical size of the publication (number of entries).
     * @param ignoredSyntheticSignatures The number of synthetic signatures that were ignored.
     * @param duration The time it took to import it.
     */
    fun recordImportedPublication(
        type: PublicationType,
        logicalSize: Int,
        ignoredSyntheticSignatures: Int,
        duration: Duration
    )

    /** Count that an agent has polled  */
    fun countAgentPoll()

    /** Count how many rows were deleted by the weeding service  */
    fun countWeededRows(deletedRows: Int)
}