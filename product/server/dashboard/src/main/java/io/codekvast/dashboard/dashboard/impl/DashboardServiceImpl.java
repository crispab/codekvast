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
package io.codekvast.dashboard.dashboard.impl;

import static java.time.temporal.ChronoUnit.DAYS;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.security.CustomerIdProvider;
import io.codekvast.dashboard.dashboard.DashboardService;
import io.codekvast.dashboard.dashboard.model.methods.ApplicationDescriptor;
import io.codekvast.dashboard.dashboard.model.methods.EnvironmentDescriptor;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsFormData;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsRequest;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsResponse2;
import io.codekvast.dashboard.dashboard.model.methods.MethodDescriptor1;
import io.codekvast.dashboard.dashboard.model.methods.MethodDescriptor2;
import io.codekvast.dashboard.dashboard.model.status.AgentDescriptor;
import io.codekvast.dashboard.dashboard.model.status.ApplicationDescriptor2;
import io.codekvast.dashboard.dashboard.model.status.EnvironmentStatusDescriptor;
import io.codekvast.dashboard.dashboard.model.status.GetStatusResponse;
import io.codekvast.javaagent.model.v2.SignatureStatus2;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** @author olle.hallin@crisp.se */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class DashboardServiceImpl implements DashboardService {

  private static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;
  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private final CustomerIdProvider customerIdProvider;
  private final CustomerService customerService;
  private final Clock clock;

  @Override
  @Transactional(readOnly = true)
  public GetMethodsResponse2 getMethods2(@Valid GetMethodsRequest request) {
    long startedAt = clock.millis();
    Long customerId = customerIdProvider.getCustomerId();
    PricePlan pricePlan = customerService.getCustomerDataByCustomerId(customerId).getPricePlan();

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue(
        "latestCollectedSince", clock.instant().minus(request.getMinCollectedDays(), DAYS));
    params.addValue("now", new Timestamp(clock.millis()));
    params.addValue("customerId", customerId);
    String whereClause = "i.customerId = :customerId AND m.modifiers NOT LIKE '%abstract%'";

    String normalizedSignature = request.getNormalizedSignature();
    if (!normalizedSignature.equals("%")) {
      params.addValue("signature", normalizedSignature);
      whereClause +=
          " AND m.signature LIKE :signature COLLATE utf8mb4_general_ci"; // Make it case-insensitive
    }
    if (request.getApplications() != null && !request.getApplications().isEmpty()) {
      params.addValue(
          "applicationIds", translateNamesToIds("applications", "name", request.getApplications()));
      whereClause += " AND i.applicationId IN (:applicationIds)";
    }
    if (request.getEnvironments() != null && !request.getEnvironments().isEmpty()) {
      params.addValue(
          "environmentIds", translateNamesToIds("environments", "name", request.getEnvironments()));
      whereClause += " AND i.environmentId IN (:environmentIds)";
    }
    if (request.getLocations() != null && !request.getLocations().isEmpty()) {
      params.addValue(
          "locationIds",
          translateNamesToIds("method_locations", "locationNoVersion", request.getLocations()));
      whereClause += " AND ml.id IN (:locationIds)";
    }

    String sql =
        "SELECT m.id, m.signature, "
            + "MAX(i.createdAt) AS latestCollectedSince, "
            + "MAX(i.status) AS status, "
            + "MAX(i.invokedAtMillis) AS lastInvokedAtMillis, "
            + "MAX(i.timestamp) AS lastPublishedAt, "
            + "m.annotation AS methodAnnotation, "
            + "ml.annotation AS methodLocationAnnotation, "
            + "p.annotation AS packageAnnotation, "
            + "t.annotation AS typeAnnotation "
            + "FROM invocations i "
            + "  INNER JOIN methods m ON i.methodId = m.id AND m.customerId = i.customerId "
            + "  INNER JOIN types t ON m.declaringType = t.name AND t.customerId = m.customerId "
            + "  INNER JOIN packages p ON m.packageName = p.name AND p.customerId = m.customerId "
            + "  LEFT JOIN method_locations ml ON ml.methodId = m.id AND ml.customerId = m.customerId "
            + "WHERE "
            + whereClause
            + " GROUP BY i.methodId "
            + "HAVING latestCollectedSince <= :latestCollectedSince "
            + "ORDER BY lastInvokedAtMillis ";

    List<MethodDescriptor2> methods = new ArrayList<>(request.getMaxResults());

    namedParameterJdbcTemplate.query(
        sql,
        params,
        rs -> {
          if (methods.size() >= request.getMaxResults()) {
            logger.trace("Ignoring row {}, since max result already achieved", rs.getRow());
            return;
          }

          String signature = rs.getString("signature");

          SignatureStatus2 status = SignatureStatus2.valueOf(rs.getString("status"));
          if (request.isSuppressUntrackedMethods() && !status.isTracked()) {
            logger.trace("Suppressing untracked method {} with status {}", signature, status);
            return;
          }

          Long lastInvokedAtMillis =
              pricePlan.adjustTimestampMillis(rs.getLong("lastInvokedAtMillis"), clock);
          if (lastInvokedAtMillis > request.getOnlyInvokedBeforeMillis()) {
            logger.trace("Suppressing method invoked after requested range");
            return;
          }
          if (lastInvokedAtMillis < request.getOnlyInvokedAfterMillis()) {
            logger.trace("Suppressing method invoked before requested range");
            return;
          }

          methods.add(
              MethodDescriptor2.builder()
                  .id(rs.getLong("id"))
                  .signature(signature)
                  .trackedPercent(status.isTracked() ? 100 : 0)
                  .collectedDays(
                      pricePlan.adjustCollectedDays(
                          getCollectedDays(rs.getTimestamp("latestCollectedSince"))))
                  .lastInvokedAtMillis(lastInvokedAtMillis)
                  .collectedToMillis(rs.getTimestamp("lastPublishedAt").getTime())
                  .methodAnnotation(rs.getString("methodAnnotation"))
                  .methodLocationAnnotation(rs.getString("methodLocationAnnotation"))
                  .typeAnnotation(rs.getString("typeAnnotation"))
                  .packageAnnotation(rs.getString("packageAnnotation"))
                  .build());
        });

    methods.sort(Comparator.comparing(MethodDescriptor2::getSignature));

    long queryTimeMillis = clock.millis() - startedAt;
    logger.debug("Processed {} in {} ms.", request, queryTimeMillis);

    return GetMethodsResponse2.builder()
        .timestamp(startedAt)
        .request(request)
        .numMethods(methods.size())
        .methods(methods)
        .queryTimeMillis(queryTimeMillis)
        .build();
  }

  private int getCollectedDays(Timestamp latestCollectedSince) {
    long durationMillis = clock.millis() - latestCollectedSince.getTime();
    return (int) (durationMillis / ONE_DAY_IN_MILLIS);
  }

  private List<Long> translateNamesToIds(
      final String tableName, String columnName, Collection<String> names) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("customerId", customerIdProvider.getCustomerId());
    params.addValue("names", new TreeSet<>(names));

    List<Long> ids =
        namedParameterJdbcTemplate.queryForList(
            String.format(
                "SELECT id FROM %s WHERE customerId = :customerId AND %s IN (:names)",
                tableName, columnName),
            params,
            Long.class);
    logger.debug(
        "Mapped {}.{} in ({}) to {} for the customer {}",
        tableName,
        columnName,
        names,
        ids,
        customerIdProvider.getCustomerId());
    return ids;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MethodDescriptor1> getMethodById(@NotNull Long methodId) {
    Long customerId = customerIdProvider.getCustomerId();
    PricePlan pricePlan = customerService.getCustomerDataByCustomerId(customerId).getPricePlan();

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("customerId", customerId);
    params.addValue("methodId", methodId);

    MethodDescriptorRowCallbackHandler rch = new MethodDescriptorRowCallbackHandler(pricePlan);
    namedParameterJdbcTemplate.query(
        "SELECT i.methodId, a.name AS appName, "
            + "  e.name AS envName, i.invokedAtMillis, i.createdAt, i.status, ad.collectedSince, ad.collectedTo, "
            + "  m.visibility, m.signature, m.declaringType, m.methodName, m.bridge, m.synthetic, m.modifiers,  m.packageName, ml.location, "
            + "  m.annotation AS methodAnnotation, ml.annotation AS methodLocationAnnotation, "
            + "  t.annotation AS typeAnnotation, p.annotation AS packageAnnotation "
            + "  FROM invocations i "
            + "    INNER JOIN applications a ON a.id = i.applicationId  "
            + "    INNER JOIN environments e ON e.id = i.environmentId  "
            + "    INNER JOIN methods m ON m.id = i.methodId  "
            + "    INNER JOIN types t ON t.name = m.declaringType "
            + "    INNER JOIN packages p ON p.name = m.packageName "
            + "    INNER JOIN application_descriptors ad ON ad.applicationId = i.applicationId AND ad.environmentId = i.environmentId "
            + "    LEFT JOIN method_locations ml ON m.id = ml.methodId  "
            + "  WHERE i.customerId = :customerId AND i.methodId = :methodId ",
        params,
        rch);

    return rch.getResult();
  }

  @Override
  @Transactional(readOnly = true)
  public GetStatusResponse getStatus() {
    long startedAt = clock.millis();

    Long customerId = customerIdProvider.getCustomerId();
    CustomerData customerData = customerService.getCustomerDataByCustomerId(customerId);

    PricePlan pricePlan = customerData.getPricePlan();
    List<EnvironmentStatusDescriptor> environments = getEnvironments(customerId);
    List<ApplicationDescriptor2> applications = getApplications(customerId, pricePlan);
    List<AgentDescriptor> agents = getAgents(customerId, pricePlan.getPublishIntervalSeconds());

    Instant now = clock.instant();
    Instant collectionStartedAt = customerData.getCollectionStartedAt();
    Instant trialPeriodEndsAt = customerData.getTrialPeriodEndsAt();
    Duration trialPeriodDuration =
        collectionStartedAt == null || trialPeriodEndsAt == null
            ? null
            : Duration.between(collectionStartedAt, trialPeriodEndsAt);
    Duration trialPeriodProgress =
        trialPeriodDuration == null ? null : Duration.between(collectionStartedAt, now);

    Integer trialPeriodPercent =
        trialPeriodProgress == null
            ? null
            : Math.min(
                100,
                Math.toIntExact(
                    trialPeriodProgress.toMillis() * 100L / trialPeriodDuration.toMillis()));

    return GetStatusResponse.builder()
        // query stuff
        .timestamp(startedAt)
        .queryTimeMillis(clock.millis() - startedAt)

        // price plan stuff
        .pricePlan(pricePlan.getName())
        .retentionPeriodDays(pricePlan.getRetentionPeriodDays())
        .collectionResolutionSeconds(pricePlan.getPublishIntervalSeconds())
        .maxNumberOfAgents(pricePlan.getMaxNumberOfAgents())
        .maxNumberOfMethods(pricePlan.getMaxMethods())

        // actual values
        .collectedSinceMillis(pricePlan.adjustInstantToMillis(collectionStartedAt, clock))
        .trialPeriodEndsAtMillis(
            trialPeriodEndsAt == null ? null : trialPeriodEndsAt.toEpochMilli())
        .trialPeriodExpired(customerData.isTrialPeriodExpired(now))
        .trialPeriodPercent(trialPeriodPercent)
        .numMethods(customerService.countMethods(customerId))
        .numAgents(agents.size())
        .numLiveAgents((int) agents.stream().filter(AgentDescriptor::isAgentAlive).count())
        .numLiveEnabledAgents(
            (int) agents.stream().filter(AgentDescriptor::isAgentLiveAndEnabled).count())

        // details
        .environments(environments)
        .applications(applications)
        .agents(agents)
        .build();
  }

  private List<EnvironmentStatusDescriptor> getEnvironments(Long customerId) {
    val startedAt = clock.instant();
    List<EnvironmentStatusDescriptor> result = new ArrayList<>();
    jdbcTemplate.query(
        "SELECT name, enabled, updatedBy, notes FROM environments WHERE customerId = ? ",
        rs -> {
          result.add(
              EnvironmentStatusDescriptor.builder()
                  .name(rs.getString("name"))
                  .enabled(rs.getBoolean("enabled"))
                  .updatedBy(rs.getString("updatedBy"))
                  .notes(rs.getString("notes"))
                  .build());
        },
        customerId);

    logger.debug(
        "Fetched {} environments for customer {} in {}",
        result.size(),
        customerId,
        Duration.between(startedAt, clock.instant()));

    return result;
  }

  private List<ApplicationDescriptor2> getApplications(Long customerId, PricePlan pricePlan) {
    val startedAt = clock.instant();
    List<ApplicationDescriptor2> result = new ArrayList<>();

    jdbcTemplate.query(
        "SELECT a.name AS appName, e.name AS envName, ad.collectedSince, ad.collectedTo\n"
            + "FROM application_descriptors ad\n"
            + "       INNER JOIN applications a ON ad.applicationId = a.id\n"
            + "       INNER JOIN environments e ON ad.environmentId = e.id\n"
            + "WHERE ad.customerId = ?\n"
            + "GROUP BY appName, envName\n"
            + "ORDER BY appName, envName\n",
        rs -> {
          result.add(
              ApplicationDescriptor2.builder()
                  .appName(rs.getString("appName"))
                  .environment(rs.getString("envName"))
                  .collectedSinceMillis(
                      pricePlan.adjustTimestampMillis(
                          rs.getTimestamp("collectedSince").getTime(), clock))
                  .collectedToMillis(
                      pricePlan.adjustTimestampMillis(
                          rs.getTimestamp("collectedTo").getTime(), clock))
                  .build());
        },
        customerId);

    logger.debug(
        "Fetched {} applications for customer {} in {}",
        result.size(),
        customerId,
        Duration.between(startedAt, clock.instant()));

    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public @NotNull GetMethodsFormData getMethodsFormData() {
    Long customerId = customerIdProvider.getCustomerId();
    CustomerData customerData = customerService.getCustomerDataByCustomerId(customerId);

    List<String> applications =
        jdbcTemplate.queryForList(
            "SELECT DISTINCT name FROM applications WHERE customerId = ? ",
            String.class,
            customerId);

    List<String> environments =
        jdbcTemplate.queryForList(
            "SELECT DISTINCT name FROM environments WHERE customerId = ? ",
            String.class,
            customerId);
    List<String> locations =
        jdbcTemplate.queryForList(
            "SELECT DISTINCT locationNoVersion FROM method_locations WHERE customerId = ? ",
            String.class,
            customerId);
    return GetMethodsFormData.builder()
        .applications(applications)
        .environments(environments)
        .locations(locations)
        .retentionPeriodDays(customerData.getPricePlan().getRetentionPeriodDays())
        .build();
  }

  private List<AgentDescriptor> getAgents(Long customerId, int publishIntervalSeconds) {
    val startedAt = clock.instant();
    List<AgentDescriptor> result = new ArrayList<>();

    jdbcTemplate.query(
        "SELECT agent_state.id AS agentId, agent_state.enabled, agent_state.lastPolledAt, agent_state.nextPollExpectedAt, jvms.id AS "
            + "jvmId,\n"
            + "       jvms.startedAt, jvms.publishedAt, jvms.methodVisibility, jvms.packages, jvms.excludePackages, jvms.hostname, jvms"
            + ".agentVersion,\n"
            + "       jvms.tags, jvms.applicationVersion AS appVersion, applications.name AS appName, environments.name AS envName\n"
            + "FROM agent_state, jvms, applications, environments\n"
            + "WHERE jvms.customerId = ? AND jvms.uuid = agent_state.jvmUuid AND jvms.applicationId = applications.id AND\n"
            + "        jvms.environmentId = environments.id AND jvms.garbage = FALSE AND agent_state.garbage = FALSE\n"
            + "ORDER BY jvms.id ",
        rs -> {
          Timestamp lastPolledAt = rs.getTimestamp("lastPolledAt");
          Timestamp nextPollExpectedAt = rs.getTimestamp("nextPollExpectedAt");
          Timestamp publishedAt = rs.getTimestamp("publishedAt");
          boolean isAlive =
              nextPollExpectedAt != null
                  && nextPollExpectedAt.after(Timestamp.from(clock.instant().minusSeconds(60)));
          Instant nextPublicationExpectedAt =
              lastPolledAt.toInstant().plusSeconds(publishIntervalSeconds);

          result.add(
              AgentDescriptor.builder()
                  .agentId(rs.getLong("agentId"))
                  .agentAlive(isAlive)
                  .agentLiveAndEnabled(isAlive && rs.getBoolean("enabled"))
                  .agentVersion(rs.getString("agentVersion"))
                  .appName(rs.getString("appName"))
                  .appVersion(rs.getString("appVersion"))
                  .environment(rs.getString("envName"))
                  .excludePackages(rs.getString("excludePackages"))
                  .jvmId(rs.getLong("jvmId"))
                  .hostname(rs.getString("hostname"))
                  .methodVisibility(rs.getString("methodVisibility"))
                  .nextPublicationExpectedAtMillis(nextPublicationExpectedAt.toEpochMilli())
                  .nextPollExpectedAtMillis(nextPollExpectedAt.getTime())
                  .packages(rs.getString("packages"))
                  .pollReceivedAtMillis(lastPolledAt.getTime())
                  .publishedAtMillis(publishedAt.getTime())
                  .startedAtMillis(rs.getTimestamp("startedAt").getTime())
                  .tags(rs.getString("tags"))
                  .build());
        },
        customerId);

    logger.debug(
        "Fetched {} agents for customer {} in {}",
        result.size(),
        customerId,
        Duration.between(startedAt, clock.instant()));

    return result;
  }

  @RequiredArgsConstructor
  private static class QueryState {
    private final long methodId;

    private final Map<String, ApplicationDescriptor> applications = new HashMap<>();
    private final Map<String, EnvironmentDescriptor> environments = new HashMap<>();

    private MethodDescriptor1.MethodDescriptor1Builder builder;
    private int rows;

    MethodDescriptor1.MethodDescriptor1Builder getBuilder() {
      if (builder == null) {
        builder = MethodDescriptor1.builder().id(methodId);
      }
      return builder;
    }

    boolean isSameMethod(long id) {
      return id == this.methodId;
    }

    void saveApplication(ApplicationDescriptor applicationDescriptor) {
      String name = applicationDescriptor.getName();
      applications.put(name, applicationDescriptor.mergeWith(applications.get(name)));
    }

    void saveEnvironment(EnvironmentDescriptor environmentDescriptor) {
      String name = environmentDescriptor.getName();
      environments.put(name, environmentDescriptor.mergeWith(environments.get(name)));
    }

    void addTo(List<MethodDescriptor1> result) {
      if (builder != null) {
        logger.trace(
            "Adding method {} to result (compiled from {} result set rows)", methodId, rows);
        builder.occursInApplications(new TreeSet<>(applications.values()));
        builder.collectedInEnvironments(new TreeSet<>(environments.values()));
        result.add(builder.build().computeFields());
      }
    }

    void countRow() {
      rows += 1;
    }
  }

  @RequiredArgsConstructor
  private class MethodDescriptorRowCallbackHandler implements RowCallbackHandler {
    private final PricePlan pricePlan;

    private final List<MethodDescriptor1> result = new ArrayList<>();
    private QueryState queryState = new QueryState(-1L);

    @Override
    public void processRow(ResultSet rs) throws SQLException {

      String signature = rs.getString("signature");
      boolean bridge = rs.getBoolean("bridge");
      boolean synthetic = rs.getBoolean("synthetic");

      long id = rs.getLong("methodId");
      if (!queryState.isSameMethod(id)) {
        // The query is sorted on methodId
        logger.trace("Found method {}:{}", id, signature);
        queryState.addTo(result);
        queryState = new QueryState(id);
      }

      queryState.countRow();
      long collectedSinceMillis =
          pricePlan.adjustTimestampMillis(rs.getTimestamp("ad.collectedSince").getTime(), clock);
      val collectedToMillis =
          pricePlan.adjustTimestampMillis(rs.getTimestamp("ad.collectedTo").getTime(), clock);
      long invokedAtMillis =
          pricePlan.adjustTimestampMillis(rs.getLong("i.invokedAtMillis"), clock);

      MethodDescriptor1.MethodDescriptor1Builder builder = queryState.getBuilder();

      queryState.saveApplication(
          ApplicationDescriptor.builder()
              .name(rs.getString("appName"))
              .collectedSinceMillis(collectedSinceMillis)
              .collectedToMillis(collectedToMillis)
              .invokedAtMillis(invokedAtMillis)
              .status(SignatureStatus2.valueOf(rs.getString("i.status")))
              .build());

      queryState.saveEnvironment(
          EnvironmentDescriptor.builder()
              .name(rs.getString("envName"))
              .collectedSinceMillis(collectedSinceMillis)
              .collectedToMillis(collectedToMillis)
              .invokedAtMillis(invokedAtMillis)
              .build()
              .computeFields());

      builder
          .declaringType(rs.getString("m.declaringType"))
          .modifiers(rs.getString("m.modifiers"))
          .packageName(rs.getString("m.packageName"))
          .signature(signature)
          .visibility(rs.getString("m.visibility"))
          .bridge(bridge)
          .synthetic(synthetic)
          .methodAnnotation(rs.getString("methodAnnotation"))
          .methodLocationAnnotation(rs.getString("methodLocationAnnotation"))
          .typeAnnotation(rs.getString("typeAnnotation"))
          .packageAnnotation(rs.getString("packageAnnotation"));

      // Left join stuff
      String location = rs.getString("ml.location");
      if (location != null) {
        builder.location(location);
      }
    }

    private Optional<MethodDescriptor1> getResult() {
      // Include the last method
      queryState.addTo(result);
      return result.stream().findFirst();
    }
  }
}
