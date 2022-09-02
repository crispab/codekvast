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
package io.codekvast.intake.agent.service.impl

import io.codekvast.intake.metrics.AgentStatistics
import java.time.Instant
import java.util.*

/**
 * Data Access Object used by IntakeServiceImpl.
 *
 * @author olle.hallin@crisp.se
 */
interface IntakeDAO {
    /**
     * Puts a write lock all rows in agent_state belonging to a certain customer.
     *
     * @param customerId The customer id
     */
    fun writeLockAgentStateForCustomer(customerId: Long)

    /**
     * Marks all agents that have not polled for a certain period of time as garbage.
     *
     * @param customerId The customer ID.
     * @param thisJvmUuid The JVM UUID of this (the polling) agent.
     * @param nextPollExpectedBefore Agents that are expected to poll after this instant are
     * considered alive.
     */
    fun markDeadAgentsAsGarbage(
            customerId: Long,
            thisJvmUuid: String,
            nextPollExpectedBefore: Instant
    )

    /**
     * Update this agent's timestamps.
     *
     * @param customerId The customer ID.
     * @param thisJvmUuid The JVM UUID of this (the polling) agent.
     * @param thisPollAt The instant the agent made this poll.
     * @param nextExpectedPollAt The instant the agent is supposed to poll before.
     */
    fun setAgentTimestamps(
            customerId: Long, thisJvmUuid: String, thisPollAt: Instant, nextExpectedPollAt: Instant
    )

    /**
     * Get the number of alive agents for a certain customer not counting this one.
     *
     * @param customerId The customer ID.
     * @param thisJvmUuid The JVM UUID of this (the polling) agent.
     * @param nextPollExpectedAfter The instant the agents are supposed to poll after.
     * @return Number of alive agents (excluding this).
     */
    fun getNumOtherEnabledAliveAgents(
            customerId: Long, thisJvmUuid: String, nextPollExpectedAfter: Instant
    ): Int

    /**
     * Is the environment the agent is running in enabled?
     *
     * @param customerId The customer ID.
     * @param thisJvmUuid The JVM UUID of this (the polling) agent.
     * @return true if the agent is allowed to publish data.
     */
    fun isEnvironmentEnabled(customerId: Long, thisJvmUuid: String): Boolean

    /**
     * Retrieves the name of the environment a certain JVM executes in. Used for upgrading a
     * GetConfigRequest2 to format 3.
     *
     * @param jvmUuid The JVM UUID of this (the polling) agent.
     * @return The name of the environment. Returns empty of unknown environment.
     */
    fun getEnvironmentName(jvmUuid: String): Optional<String>

    /**
     * At the end of the poll, the result that is returned to the agent should also be stored in the
     * database so that the count of live, enabled agents can be computed when the next agent polls.
     *
     * @param customerId The customer ID.
     * @param thisJvmUuid The JVM UUID of this (the polling) agent.
     * @param enabled The result of this poll.
     */
    fun updateAgentEnabledState(customerId: Long, thisJvmUuid: String, enabled: Boolean)

    /**
     * Get agent statistics.
     *
     * @param nextPollExpectedAfter The instant the agent is supposed to poll after to be considered
     * alive.
     * @return Agent statistics
     */
    fun getAgentStatistics(nextPollExpectedAfter: Instant): AgentStatistics
}
