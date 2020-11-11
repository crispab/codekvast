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
package io.codekvast.dashboard.agent.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.model.AgentPolledEvent;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.dashboard.metrics.AgentMetricsService;
import io.codekvast.dashboard.metrics.AgentStatistics;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** @author olle.hallin@crisp.se */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentStateManagerImpl implements AgentStateManager {

  private final CodekvastDashboardSettings settings;
  private final CustomerService customerService;
  private final EventService eventService;
  private final AgentDAO agentDAO;
  private final AgentMetricsService agentMetricsService;

  @Scheduled(
      initialDelayString = "${codekvast.agent-statistics.delay.seconds:60}000",
      fixedRateString = "${codekvast.agent-statistics.interval.seconds:60}000")
  @Transactional(readOnly = true)
  void countAgents() {
    AgentStatistics statistics = agentDAO.getAgentStatistics(Instant.now().minusSeconds(10));
    logger.debug("Collected {}", statistics);
    agentMetricsService.gaugeAgents(statistics);
  }

  @Override
  @Transactional
  @SneakyThrows
  public boolean updateAgentState(
      CustomerData customerData, String jvmUuid, String appName, String environment) {

    agentDAO.writeLockAgentStateForCustomer(customerData.getCustomerId());

    return doUpdateAgentState(customerData, jvmUuid, appName, environment);
  }

  private boolean doUpdateAgentState(
      CustomerData customerData, String jvmUuid, String appName, String environment) {
    long customerId = customerData.getCustomerId();
    Instant now = Instant.now();

    agentDAO.disableDeadAgents(
        customerId, jvmUuid, now.minusSeconds(settings.getFileImportIntervalSeconds() * 2));

    agentDAO.setAgentTimestamps(
        customerId,
        jvmUuid,
        now,
        now.plusSeconds(customerData.getPricePlan().getPollIntervalSeconds()));

    CustomerData cd = customerService.registerAgentPoll(customerData, now);
    int numOtherEnabledLiveAgents =
        agentDAO.getNumOtherAliveAgents(customerId, jvmUuid, now.minusSeconds(10));

    val event =
        AgentPolledEvent.builder()
            .afterTrialPeriod(cd.isTrialPeriodExpired(now))
            .appName(appName)
            .customerId(customerId)
            .disabledEnvironment(!agentDAO.isEnvironmentEnabled(customerId, jvmUuid))
            .environment(environment)
            .jvmUuid(jvmUuid)
            .polledAt(now)
            .tooManyLiveAgents(
                numOtherEnabledLiveAgents >= customerData.getPricePlan().getMaxNumberOfAgents())
            .trialPeriodEndsAt(cd.getTrialPeriodEndsAt())
            .build();

    logger.debug("Agent {} is {}", jvmUuid, event.isAgentEnabled() ? "enabled" : "disabled");
    eventService.send(event);
    agentDAO.updateAgentEnabledState(customerId, jvmUuid, event.isAgentEnabled());
    return event.isAgentEnabled();
  }
}
