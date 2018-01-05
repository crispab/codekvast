/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.dashboard.webapp.impl;

import io.codekvast.dashboard.customer.CustomerData;
import io.codekvast.dashboard.customer.CustomerService;
import io.codekvast.dashboard.customer.PricePlan;
import io.codekvast.dashboard.security.CustomerIdProvider;
import io.codekvast.dashboard.util.TimeService;
import io.codekvast.dashboard.webapp.WebappService;
import io.codekvast.dashboard.webapp.model.methods.*;
import io.codekvast.dashboard.webapp.model.status.AgentDescriptor1;
import io.codekvast.dashboard.webapp.model.status.GetStatusResponse1;
import io.codekvast.dashboard.webapp.model.status.UserDescriptor1;
import io.codekvast.javaagent.model.v2.SignatureStatus2;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class WebappServiceImpl implements WebappService {

    private static final String UNKNOWN_ENVIRONMENT = "<unknown>";

    private final JdbcTemplate jdbcTemplate;
    private final CustomerIdProvider customerIdProvider;
    private final CustomerService customerService;
    private final TimeService timeService;

    @Override
    @Transactional(readOnly = true)
    public GetMethodsResponse getMethods(@Valid GetMethodsRequest request) {
        long startedAt = timeService.currentTimeMillis();

        MethodDescriptorRowCallbackHandler rowCallbackHandler =
            new MethodDescriptorRowCallbackHandler("m.signature LIKE ?");

        jdbcTemplate.query(rowCallbackHandler.getSelectStatement(), rowCallbackHandler, customerIdProvider.getCustomerId(),
                           request.getNormalizedSignature());

        List<MethodDescriptor> methods = rowCallbackHandler.getResult(request);


        long queryTimeMillis = timeService.currentTimeMillis() - startedAt;
        logger.debug("Processed {} in {} ms. {} result set rows processed.", request, queryTimeMillis, rowCallbackHandler.getRowCount());

        return GetMethodsResponse.builder()
                                 .timestamp(startedAt)
                                 .request(request)
                                 .numMethods(methods.size())
                                 .methods(methods)
                                 .queryTimeMillis(queryTimeMillis)
                                 .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MethodDescriptor> getMethodById(@NotNull Long methodId) {
        GetMethodsRequest request = GetMethodsRequest.defaults().toBuilder()
                                                     .maxResults(1)
                                                     .suppressUntrackedMethods(false)
                                                     .suppressSyntheticMethods(false)
                                                     .minCollectedDays(0)
                                                     .build();
        MethodDescriptorRowCallbackHandler rowCallbackHandler = new MethodDescriptorRowCallbackHandler("m.id = ?");

        jdbcTemplate.query(rowCallbackHandler.getSelectStatement(), rowCallbackHandler, customerIdProvider.getCustomerId(), methodId);

        return rowCallbackHandler.getResult(request).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public GetStatusResponse1 getStatus() {
        long startedAt = timeService.currentTimeMillis();

        Long customerId = customerIdProvider.getCustomerId();
        CustomerData customerData = customerService.getCustomerDataByCustomerId(customerId);

        PricePlan pp = customerData.getPricePlan();
        List<AgentDescriptor1> agents = getAgents(customerId, pp.getPublishIntervalSeconds());
        List<UserDescriptor1> users = getUsers(customerId);

        Instant now = timeService.now();
        Instant collectionStartedAt = customerData.getCollectionStartedAt();
        Instant trialPeriodEndsAt = customerData.getTrialPeriodEndsAt();
        Duration trialPeriodDuration =
            collectionStartedAt == null || trialPeriodEndsAt == null ? null : Duration.between(collectionStartedAt, trialPeriodEndsAt);
        Duration trialPeriodProgress = trialPeriodDuration == null ? null : Duration.between(collectionStartedAt, now);

        Integer trialPeriodPercent = trialPeriodProgress == null ? null :
            Math.min(100, Math.toIntExact(trialPeriodProgress.toMillis() * 100L / trialPeriodDuration.toMillis()));

        long dayInMillis = 24 * 60 * 60 * 1000L;
        Integer collectedDays =
            collectionStartedAt == null ? null : Math.toIntExact(Duration.between(collectionStartedAt, now).toMillis() / dayInMillis);

        return GetStatusResponse1.builder()
                                 // query stuff
                                 .timestamp(startedAt)
                                 .queryTimeMillis(timeService.currentTimeMillis() - startedAt)

                                 // price plan stuff
                                 .pricePlan(pp.getName())
                                 .collectionResolutionSeconds(pp.getPublishIntervalSeconds())
                                 .maxNumberOfAgents(pp.getMaxNumberOfAgents())
                                 .maxNumberOfMethods(pp.getMaxMethods())

                                 // actual values
                                 .collectedSinceMillis(collectionStartedAt == null ? null : collectionStartedAt.toEpochMilli())
                                 .trialPeriodEndsAtMillis(trialPeriodEndsAt == null ? null : trialPeriodEndsAt.toEpochMilli())
                                 .trialPeriodExpired(customerData.isTrialPeriodExpired(now))
                                 .trialPeriodPercent(trialPeriodPercent)
                                 .collectedDays(collectedDays)
                                 .numMethods(customerService.countMethods(customerId))
                                 .numAgents(agents.size())
                                 .numLiveAgents((int) agents.stream().filter(AgentDescriptor1::isAgentAlive).count())
                                 .numLiveEnabledAgents((int) agents.stream().filter(AgentDescriptor1::isAgentLiveAndEnabled).count())

                                 // details
                                 .agents(agents)
                                 .users(users)
                                 .build();
    }

    private List<UserDescriptor1> getUsers(Long customerId) {
        List<UserDescriptor1> result = new ArrayList<>();

        jdbcTemplate.query(
            "SELECT email, firstLoginAt, lastLoginAt, lastActivityAt, numberOfLogins, lastLoginSource " +
                "FROM users WHERE customerId = ? ORDER BY email ",
            rs -> {
                result.add(
                    UserDescriptor1.builder()
                                   .email(rs.getString("email"))
                                   .firstLoginAtMillis(rs.getTimestamp("firstLoginAt").getTime())
                                   .lastLoginAtMillis(rs.getTimestamp("lastLoginAt").getTime())
                                   .lastActivityAtMillis(rs.getTimestamp("lastActivityAt").getTime())
                                   .numberOfLogins(rs.getInt("numberOfLogins"))
                                   .lastLoginSource(rs.getString("lastLoginSource"))
                                   .build());
            }, customerId);
        return result;
    }

    private List<AgentDescriptor1> getAgents(Long customerId, int publishIntervalSeconds) {
        List<AgentDescriptor1> result = new ArrayList<>();

        jdbcTemplate.query(
            "SELECT agent_state.enabled, agent_state.lastPolledAt, agent_state.nextPollExpectedAt, " +
                "jvms.id AS jvmId, jvms.startedAt, jvms.publishedAt, jvms.methodVisibility, jvms.packages, jvms.excludePackages, " +
                "jvms.agentVersion, jvms.environment, jvms.tags, " +
                "applications.name AS appName, applications.version AS appVersion " +
                "FROM agent_state, jvms, applications " +
                "WHERE jvms.customerId = ? " +
                "AND jvms.uuid = agent_state.jvmUuid " +
                "AND jvms.applicationId = applications.id " +
                "ORDER BY jvms.id ",

            rs -> {
                Timestamp lastPolledAt = rs.getTimestamp("lastPolledAt");
                Timestamp nextPollExpectedAt = rs.getTimestamp("nextPollExpectedAt");
                Timestamp publishedAt = rs.getTimestamp("publishedAt");
                boolean isAlive =
                    nextPollExpectedAt != null && nextPollExpectedAt.after(Timestamp.from(timeService.now().minusSeconds(60)));
                Instant nextPublicationExpectedAt = lastPolledAt.toInstant().plusSeconds(publishIntervalSeconds);

                result.add(
                    AgentDescriptor1.builder()
                                    .agentAlive(isAlive)
                                    .agentLiveAndEnabled(isAlive && rs.getBoolean("enabled"))
                                    .agentVersion(rs.getString("agentVersion"))
                                    .appName(rs.getString("appName"))
                                    .appVersion(rs.getString("appVersion"))
                                    .environment(getStringOrDefault(rs, "environment", UNKNOWN_ENVIRONMENT))
                                    .excludePackages(rs.getString("excludePackages"))
                                    .id(rs.getLong("jvmId"))
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

    private class MethodDescriptorRowCallbackHandler implements RowCallbackHandler {
        private final String whereClause;
        private final List<MethodDescriptor> result = new ArrayList<>();

        private QueryState queryState;

        @Getter
        private int rowCount;

        private MethodDescriptorRowCallbackHandler(String whereClause) {
            this.whereClause = whereClause;
            queryState = new QueryState(-1L);
        }

        String getSelectStatement() {

            // This is a simpler to understand approach than trying to do everything in the database.
            // Let the database do the joining and selection, and the Java layer do the data reduction. The query will return several rows
            // for each method that matches the WHERE clause, and the RowCallbackHandler reduces them to only one MethodDescriptor per
            // method ID.
            // This is probably doable in pure SQL too, provided you are a black-belt SQL ninja. Unfortunately I'm not that strong at SQL.

            return String.format("SELECT i.methodId, a.name AS appName, a.version AS appVersion,\n" +
                                     "  i.invokedAtMillis, i.status, j.startedAt, j.publishedAt, j.environment, j.hostname, j.tags,\n" +
                                     "  m.visibility, m.signature, m.declaringType, m.methodName, m.bridge, m.synthetic, m.modifiers, m" +
                                     ".packageName\n" +
                                     "  FROM invocations i\n" +
                                     "  JOIN applications a ON a.id = i.applicationId \n" +
                                     "  JOIN methods m ON m.id = i.methodId\n" +
                                     "  JOIN jvms j ON j.id = i.jvmId\n" +
                                     "  WHERE i.customerId = ? AND %s\n" +
                                     "  ORDER BY i.methodId ASC", whereClause);
        }

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            this.rowCount += 1;

            long id = rs.getLong("methodId");
            String signature = rs.getString("signature");

            if (!queryState.isSameMethod(id)) {
                // The query is sorted on methodId
                logger.trace("Found method {}:{}", id, signature);
                queryState.addTo(result);
                queryState = new QueryState(id);
            }

            queryState.countRow();
            long startedAt = rs.getTimestamp("startedAt").getTime();
            long publishedAt = rs.getTimestamp("publishedAt").getTime();
            long invokedAtMillis = rs.getLong("invokedAtMillis");

            MethodDescriptor.MethodDescriptorBuilder builder = queryState.getBuilder();
            String appName = rs.getString("appName");
            String appVersion = rs.getString("appVersion");

            queryState.saveApplication(ApplicationDescriptor
                                           .builder()
                                           .name(appName)
                                           .version(appVersion)
                                           .startedAtMillis(startedAt)
                                           .publishedAtMillis(publishedAt)
                                           .invokedAtMillis(invokedAtMillis)
                                           .status(SignatureStatus2.valueOf(rs.getString("status")))
                                           .build());

            queryState.saveEnvironment(EnvironmentDescriptor.builder()
                                                            .name(getStringOrDefault(rs, "environment", UNKNOWN_ENVIRONMENT))
                                                            .hostname(rs.getString("hostname"))
                                                            .tags(splitOnCommaOrSemicolon(rs.getString("tags")))
                                                            .collectedSinceMillis(startedAt)
                                                            .collectedToMillis(publishedAt)
                                                            .invokedAtMillis(invokedAtMillis)
                                                            .build());

            builder.declaringType(rs.getString("declaringType"))
                   .modifiers(rs.getString("modifiers"))
                   .packageName(rs.getString("packageName"))
                   .signature(signature)
                   .visibility(rs.getString("visibility"))
                   .bridge(rs.getBoolean("bridge"))
                   .synthetic(rs.getBoolean("synthetic"));
        }

        private Set<String> splitOnCommaOrSemicolon(String tags) {
            return new HashSet<>(Arrays.asList(tags.split("\\s*[,;]\\s")));
        }

        private List<MethodDescriptor> getResult(GetMethodsRequest request) {
            // Include the last method
            queryState.addTo(result);

            // Get rid of unwanted result
            for (Iterator<MethodDescriptor> iterator = result.iterator(); iterator.hasNext(); ) {
                MethodDescriptor md = iterator.next();
                boolean keep = true;

                if (request.isSuppressSyntheticMethods() && (md.getBridge() || md.getSynthetic() || isSyntheticMethod(md.getSignature()))) {
                    logger.trace("Throwing away synthetic method: {}", md);
                    keep = false;
                }
                if (keep && request.isSuppressUntrackedMethods() && md.getStatuses().stream().anyMatch(s -> !s.isTracked())) {
                    logger.trace("Throwing away untracked method: {}", md);
                    keep = false;
                }

                if (keep && md.getCollectedDays() < request.getMinCollectedDays()) {
                    logger.trace("Throwing away method collected too few days: {}", md);
                    keep = false;
                }

                // only calculate once (if needed)
                Long lastInvokedAtMillis = keep ? md.getLastInvokedAtMillis() : 0L;

                if (keep && request.getOnlyInvokedAfterMillis() > lastInvokedAtMillis) {
                    logger.trace("Throwing away too old method: {}", md);
                    keep = false;
                }
                if (keep && request.getOnlyInvokedBeforeMillis() < lastInvokedAtMillis) {
                    logger.trace("Throwing away too new method: {}", md);
                    keep = false;
                }
                if (!keep) {
                    iterator.remove();
                } else {
                    logger.trace("Keeping {}", md);
                }
            }

            // Sort with respect to lastInvokedAt ASC (so that we keep the oldest invocations)
            result.sort((md1, md2) -> (int) (md2.getLastInvokedAtMillis() - md1.getLastInvokedAtMillis()));

            // Limit the result
            return result.stream().limit(request.getMaxResults()).collect(Collectors.toList());
        }

    }

    String getStringOrDefault(ResultSet rs, String columnLabel, String defaultValue) throws SQLException {
        String value = rs.getString(columnLabel);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    boolean isSyntheticMethod(String signature) {
        return signature.contains("..");
        // Are there any other strange patterns not containing ".." ?
    }

    @RequiredArgsConstructor
    private class QueryState {
        private final long methodId;

        private final Map<ApplicationId, ApplicationDescriptor> applications = new HashMap<>();
        private final Map<String, EnvironmentDescriptor> environments = new HashMap<>();

        private MethodDescriptor.MethodDescriptorBuilder builder;
        private int rows;

        MethodDescriptor.MethodDescriptorBuilder getBuilder() {
            if (builder == null) {
                builder = MethodDescriptor.builder().id(methodId);
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

        void addTo(List<MethodDescriptor> result) {
            if (builder != null) {
                logger.trace("Adding method {} to result (compiled from {} result set rows)", methodId, rows);
                builder.occursInApplications(new TreeSet<>(applications.values()));
                builder.collectedInEnvironments(new TreeSet<>(environments.values()));
                result.add(builder.build());
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
