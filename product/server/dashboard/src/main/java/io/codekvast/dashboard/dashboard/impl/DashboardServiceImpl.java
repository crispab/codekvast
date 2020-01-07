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
package io.codekvast.dashboard.dashboard.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.security.CustomerIdProvider;
import io.codekvast.dashboard.dashboard.DashboardService;
import io.codekvast.dashboard.dashboard.model.methods.*;
import io.codekvast.dashboard.dashboard.model.status.AgentDescriptor;
import io.codekvast.dashboard.dashboard.model.status.ApplicationDescriptor2;
import io.codekvast.dashboard.dashboard.model.status.EnvironmentStatusDescriptor;
import io.codekvast.dashboard.dashboard.model.status.GetStatusResponse;
import io.codekvast.javaagent.model.v2.SignatureStatus2;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class DashboardServiceImpl implements DashboardService {

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
        params.addValue("latestCollectedSince", clock.instant().minus(request.getMinCollectedDays(), DAYS));
        params.addValue("now", new Timestamp(clock.millis()));
        params.addValue("onlyInvokedAfterMillis", request.getOnlyInvokedAfterMillis());
        params.addValue("onlyInvokedBeforeMillis", request.getOnlyInvokedBeforeMillis());
        params.addValue("customerId", customerId);
        String whereClause = "i.customerId = :customerId";

        String normalizedSignature = request.getNormalizedSignature();
        if (!normalizedSignature.equals("%")) {
            params.addValue("signature", normalizedSignature);
            whereClause += " AND m.signature LIKE :signature COLLATE utf8mb4_general_ci"; // Make it case-insensitive
        }
        if (request.getApplications() != null && !request.getApplications().isEmpty()) {
            params.addValue("applicationIds", translateNamesToIds("applications", request.getApplications()));
            whereClause += " AND i.applicationId IN (:applicationIds)";
        }
        if (request.getEnvironments() != null && !request.getEnvironments().isEmpty()) {
            params.addValue("environmentIds", translateNamesToIds("environments", request.getEnvironments()));
            whereClause += " AND i.environmentId IN (:environmentIds)";
        }

        String sql = "SELECT m.id, m.signature, " +
            "MAX(i.createdAt) AS latestCollectedSince, " +
            "MAX(i.status) AS status, " +
            "MAX(i.invokedAtMillis) AS lastInvokedAtMillis, " +
            "MAX(i.timestamp) AS lastPublishedAt " +
            "FROM invocations i INNER JOIN methods m ON i.methodId = m.id " +
            "WHERE " + whereClause + " " +
            "GROUP BY m.id " +
            "HAVING latestCollectedSince <= :latestCollectedSince " +
            "AND lastInvokedAtMillis BETWEEN :onlyInvokedAfterMillis AND :onlyInvokedBeforeMillis\n";

        List<MethodDescriptor2> methods = new ArrayList<>(request.getMaxResults());

        namedParameterJdbcTemplate.query(sql, params, rs -> {
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

            methods.add(
                MethodDescriptor2.builder()
                                 .id(rs.getLong("id"))
                                 .signature(signature)
                                 .trackedPercent(status.isTracked() ? 100 : 0)
                                 .collectedDays(getCollectedDays(rs.getTimestamp("latestCollectedSince")))
                                 .lastInvokedAtMillis(pricePlan.adjustTimestampMillis(rs.getLong("lastInvokedAtMillis"), clock))
                                 .collectedToMillis(rs.getTimestamp("lastPublishedAt").getTime())
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
        long oneDayInMillis = 24 * 60 * 60 * 1000;
        return (int) (durationMillis / oneDayInMillis);
    }

    private List<Long> translateNamesToIds(final String tableName, Collection<String> names) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("customerId", customerIdProvider.getCustomerId());
        params.addValue("names", names);

        List<Long> ids = namedParameterJdbcTemplate
            .queryForList("SELECT id FROM " + tableName + " WHERE customerId = :customerId AND name IN (:names)", params, Long.class);
        logger.debug("Mapped {} {} to {} for the customer {}", tableName, names, ids, customerIdProvider.getCustomerId());
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
        namedParameterJdbcTemplate.query("SELECT i.methodId, a.name AS appName, j.applicationVersion AS appVersion,\n" +
                                             "  e.name AS envName, i.invokedAtMillis, i.status, j.startedAt, j.publishedAt, j.hostname, j" +
                                             ".tags,\n" +
                                             "  m.visibility, m.signature, m.declaringType, m.methodName, m.bridge, m.synthetic, m" +
                                             ".modifiers," +
                                             "  m.packageName, ml.location\n" +
                                             "  FROM invocations i\n" +
                                             "  INNER JOIN applications a ON a.id = i.applicationId \n" +
                                             "  INNER JOIN environments e ON e.id = i.environmentId \n" +
                                             "  INNER JOIN methods m ON m.id = i.methodId \n" +
                                             "  INNER JOIN jvms j ON j.applicationId = i.applicationId AND j.environmentId = i.environmentId\n" +
                                             "  LEFT JOIN method_locations ml ON m.id = ml.methodId \n" +
                                             "  WHERE i.customerId = :customerId AND i.methodId = :methodId AND j.garbage = FALSE\n",
                                         params, rch);

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
            collectionStartedAt == null || trialPeriodEndsAt == null ? null : Duration.between(collectionStartedAt, trialPeriodEndsAt);
        Duration trialPeriodProgress = trialPeriodDuration == null ? null : Duration.between(collectionStartedAt, now);

        Integer trialPeriodPercent = trialPeriodProgress == null ? null :
            Math.min(100, Math.toIntExact(trialPeriodProgress.toMillis() * 100L / trialPeriodDuration.toMillis()));

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
                                .trialPeriodEndsAtMillis(trialPeriodEndsAt == null ? null : trialPeriodEndsAt.toEpochMilli())
                                .trialPeriodExpired(customerData.isTrialPeriodExpired(now))
                                .trialPeriodPercent(trialPeriodPercent)
                                .numMethods(customerService.countMethods(customerId))
                                .numAgents(agents.size())
                                .numLiveAgents((int) agents.stream().filter(AgentDescriptor::isAgentAlive).count())
                                .numLiveEnabledAgents((int) agents.stream().filter(AgentDescriptor::isAgentLiveAndEnabled).count())

                                // details
                                .environments(environments)
                                .applications(applications)
                                .agents(agents)
                                .build();
    }

    private List<EnvironmentStatusDescriptor> getEnvironments(Long customerId) {
        List<EnvironmentStatusDescriptor> result = new ArrayList<>();
        jdbcTemplate.query("SELECT name, enabled, updatedBy, notes FROM environments " +
                               "WHERE customerId = ? ",
                           rs -> {
                               result.add(
                                   EnvironmentStatusDescriptor.builder()
                                                              .name(rs.getString("name"))
                                                              .enabled(rs.getBoolean("enabled"))
                                                              .updatedBy(rs.getString("updatedBy"))
                                                              .notes(rs.getString("notes"))
                                                              .build()
                               );
                           },
                           customerId
        );

        return result;
    }

    private List<ApplicationDescriptor2> getApplications(Long customerId, PricePlan pricePlan) {
        List<ApplicationDescriptor2> result = new ArrayList<>();

        jdbcTemplate.query(
            "SELECT a.name AS appName, e.name AS envName, MIN(i.createdAt) AS collectedSince, MAX(i.timestamp) AS collectedTo\n" +
                "FROM invocations i\n" +
                "       INNER JOIN applications a ON i.applicationId = a.id\n" +
                "       INNER JOIN environments e ON i.environmentId = e.id\n" +
                "WHERE a.customerId = ?\n" +
                "GROUP BY appName, envName\n" +
                "ORDER BY appName, envName\n",

            rs -> {
                result.add(
                    ApplicationDescriptor2.builder()
                                          .appName(rs.getString("appName"))
                                          .environment(rs.getString("envName"))
                                          .collectedSinceMillis(
                                              pricePlan.adjustTimestampMillis(rs.getTimestamp("collectedSince").getTime(), clock))
                                          .collectedToMillis(
                                              pricePlan.adjustTimestampMillis(rs.getTimestamp("collectedTo").getTime(), clock))
                                          .build());
            }, customerId);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public @NotNull GetMethodsFormData getMethodsFormData() {
        Long customerId = customerIdProvider.getCustomerId();
        CustomerData customerData = customerService.getCustomerDataByCustomerId(customerId);

        return GetMethodsFormData
            .builder()
            .applications(
                jdbcTemplate
                    .queryForList("SELECT name FROM applications WHERE customerId = ? ", String.class, customerId))
            .environments(
                jdbcTemplate.queryForList("SELECT name FROM environments WHERE customerId = ? ", String.class,
                                          customerId))
            .retentionPeriodDays(customerData.getPricePlan().getRetentionPeriodDays())
            .build();
    }

    private List<AgentDescriptor> getAgents(Long customerId, int publishIntervalSeconds) {
        List<AgentDescriptor> result = new ArrayList<>();

        jdbcTemplate.query(
            "SELECT agent_state.id AS agentId, agent_state.enabled, agent_state.lastPolledAt, agent_state.nextPollExpectedAt, jvms.id AS " +
                "jvmId,\n" +
                "       jvms.startedAt, jvms.publishedAt, jvms.methodVisibility, jvms.packages, jvms.excludePackages, jvms.hostname, jvms" +
                ".agentVersion,\n" +
                "       jvms.tags, jvms.applicationVersion AS appVersion, applications.name AS appName, environments.name AS envName\n" +
                "FROM agent_state, jvms, applications, environments\n" +
                "WHERE jvms.customerId = ? AND jvms.uuid = agent_state.jvmUuid AND jvms.applicationId = applications.id AND\n" +
                "        jvms.environmentId = environments.id AND jvms.garbage = FALSE AND agent_state.garbage = FALSE\n" +
                "ORDER BY jvms.id ",

            rs -> {
                Timestamp lastPolledAt = rs.getTimestamp("lastPolledAt");
                Timestamp nextPollExpectedAt = rs.getTimestamp("nextPollExpectedAt");
                Timestamp publishedAt = rs.getTimestamp("publishedAt");
                boolean isAlive =
                    nextPollExpectedAt != null && nextPollExpectedAt.after(Timestamp.from(clock.instant().minusSeconds(60)));
                Instant nextPublicationExpectedAt = lastPolledAt.toInstant().plusSeconds(publishIntervalSeconds);

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
            }, customerId);

        return result;
    }

    @RequiredArgsConstructor
    private class MethodDescriptorRowCallbackHandler implements RowCallbackHandler {
        private final PricePlan pricePlan;

        private final List<MethodDescriptor1> result = new ArrayList<>();
        private QueryState queryState = new QueryState(-1L);

        @Getter
        private int rowCount;

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            this.rowCount += 1;

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
            long startedAt = pricePlan.adjustTimestampMillis(rs.getTimestamp("j.startedAt").getTime(), clock);
            long publishedAt = pricePlan.adjustTimestampMillis(rs.getTimestamp("j.publishedAt").getTime(), clock);
            long invokedAtMillis = pricePlan.adjustTimestampMillis(rs.getLong("i.invokedAtMillis"), clock);

            MethodDescriptor1.MethodDescriptor1Builder builder = queryState.getBuilder();
            String appName = rs.getString("appName");
            String appVersion = rs.getString("appVersion");

            queryState.saveApplication(ApplicationDescriptor
                                           .builder()
                                           .name(appName)
                                           .version(appVersion)
                                           .startedAtMillis(startedAt)
                                           .publishedAtMillis(publishedAt)
                                           .invokedAtMillis(invokedAtMillis)
                                           .status(SignatureStatus2.valueOf(rs.getString("i.status")))
                                           .build());

            queryState.saveEnvironment(EnvironmentDescriptor.builder()
                                                            .name(rs.getString("envName"))
                                                            .hostname(rs.getString("j.hostname"))
                                                            .tags(splitOnCommaOrSemicolon(rs.getString("j.tags")))
                                                            .collectedSinceMillis(startedAt)
                                                            .collectedToMillis(publishedAt)
                                                            .invokedAtMillis(invokedAtMillis)
                                                            .build().computeFields());

            builder.declaringType(rs.getString("m.declaringType"))
                   .modifiers(rs.getString("m.modifiers"))
                   .packageName(rs.getString("m.packageName"))
                   .signature(signature)
                   .visibility(rs.getString("m.visibility"))
                   .bridge(bridge)
                   .synthetic(synthetic);

            // Left join stuff
            String location = rs.getString("ml.location");
            if (location != null) {
                builder.location(location);
            }
        }

        private Set<String> splitOnCommaOrSemicolon(String tags) {
            return new HashSet<>(Arrays.asList(tags.split("\\s*[,;]\\s")));
        }

        private Optional<MethodDescriptor1> getResult() {
            // Include the last method
            queryState.addTo(result);
            return result.stream().findFirst();
        }

    }

    @RequiredArgsConstructor
    private static class QueryState {
        private final long methodId;

        private final Map<ApplicationId, ApplicationDescriptor> applications = new HashMap<>();
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
            ApplicationId appId = ApplicationId.of(applicationDescriptor);
            applications.put(appId, applicationDescriptor.mergeWith(applications.get(appId)));

        }

        void saveEnvironment(EnvironmentDescriptor environmentDescriptor) {
            String name = environmentDescriptor.getName();
            environments.put(name, environmentDescriptor.mergeWith(environments.get(name)));
        }

        void addTo(List<MethodDescriptor1> result) {
            if (builder != null) {
                logger.trace("Adding method {} to result (compiled from {} result set rows)", methodId, rows);
                builder.occursInApplications(new TreeSet<>(applications.values()));
                builder.collectedInEnvironments(new TreeSet<>(environments.values()));
                result.add(builder.build().computeFields());
            }
        }

        void countRow() {
            rows += 1;
        }
    }

    /**
     * @author olle.hallin@crisp.se
     */
    @Value
    static class ApplicationId implements Comparable<ApplicationId> {
        private final String name;
        private final String version;

        @Override
        public int compareTo(ApplicationId that) {
            return this.toString().compareTo(that.toString());
        }

        static ApplicationId of(ApplicationDescriptor applicationDescriptor) {
            return new ApplicationId(applicationDescriptor.getName(), applicationDescriptor.getVersion());
        }
    }
}
