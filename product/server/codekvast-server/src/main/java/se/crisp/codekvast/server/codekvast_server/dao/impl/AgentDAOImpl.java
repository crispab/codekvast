package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.daemon_api.model.v1.JvmData;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureData;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.event.display.*;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.ApplicationSettingsEntry;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.OrganisationSettings;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DAO for agent stuff.
 *
 * @author olle.hallin@crisp.se
 */
@Repository
@Slf4j
public class AgentDAOImpl extends AbstractDAOImpl implements AgentDAO {

    private final CodekvastSettings codekvastSettings;

    @Inject
    public AgentDAOImpl(JdbcTemplate jdbcTemplate, CodekvastSettings codekvastSettings) {
        super(jdbcTemplate);
        this.codekvastSettings = codekvastSettings;
    }

    @Override
    @Cacheable("agent")
    public long getAppId(long organisationId, String appName) throws UndefinedApplicationException {
        log.debug("Looking up app id for {}:{}", organisationId, appName);
        return doGetOrCreateApp(organisationId, appName);
    }

    private Long doGetOrCreateApp(long organisationId, String appName)
            throws UndefinedApplicationException {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM applications WHERE organisation_id = ? AND name = ? ",
                                               Long.class, organisationId, appName);
        } catch (EmptyResultDataAccessException ignored) {
        }

        long appId = doInsertRow("INSERT INTO applications(organisation_id, name, usage_cycle_seconds) VALUES(?, ?, ?)",
                                 organisationId, appName, codekvastSettings.getDefaultTrulyDeadAfterSeconds());

        log.info("Created application {}: '{}'", appId, appName);
        return appId;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("agent")
    public AppId getAppIdByJvmUuid(String jvmUuid) {
        log.debug("Looking up AppId for JVM {}...", jvmUuid);
        try {
            AppId result = jdbcTemplate
                    .queryForObject("SELECT id, organisation_id, application_id, application_version FROM jvm_info WHERE jvm_uuid = ?",
                                    new AppIdRowMapper(),
                                    jvmUuid);
            log.debug("Result = {}", result);
            return result;
        } catch (EmptyResultDataAccessException e) {
            log.info("No AppId found for JVM {}, probably an agent that uploaded stale data", jvmUuid);
            return null;
        }
    }

    @Override
    @Cacheable("agent")
    public Collection<AppId> getApplicationIds(long organisationId, Collection<String> applicationNames) {
        String condition = applicationNames.size() == 1 ? "= ?"
                : "IN (" + applicationNames.stream().map(s -> "?").collect(Collectors.joining(",")) + ")";

        List<Object> args = new ArrayList<>();
        args.add(organisationId);
        args.addAll(applicationNames);

        return jdbcTemplate.query("SELECT jvm.id, jvm.organisation_id, jvm.application_id, jvm.application_version " +
                                          "FROM jvm_info jvm, applications a " +
                                          "WHERE jvm.application_id = a.id " +
                                          "AND jvm.organisation_id = ? " +
                                          "AND a.name " + condition,
                                  new AppIdRowMapper(), args.toArray());
    }

    @Override
    public Collection<AppId> getApplicationIds(long organisationId, String appName, String appVersion, String hostname) {
        return jdbcTemplate.query("SELECT jvm.id, jvm.organisation_id, jvm.application_id, jvm.application_version " +
                                          "FROM jvm_info jvm, applications a " +
                                          "WHERE jvm.application_id = a.id " +
                                          "AND jvm.organisation_id = ? " +
                                          "AND a.name = ? " +
                                          "AND jvm.application_version = ? " +
                                          "AND jvm.collector_host_name = ? ",
                                  new AppIdRowMapper(), organisationId, appName, appVersion, hostname);
    }

    private static class AppIdRowMapper implements RowMapper<AppId> {
        @Override
        public AppId mapRow(ResultSet rs, int rowNum) throws SQLException {
            return AppId.builder().jvmId(rs.getLong(1)).organisationId(rs.getLong(2)).appId(rs.getLong(3))
                        .appVersion(rs.getString(4)).build();
        }
    }


    @Override
    public void storeInvocationData(AppId appId, SignatureData signatureData) {

        long agentClockSkewMillis = calculateAgentClockSkewMillis(signatureData.getDaemonTimeMillis());

        List<Object[]> args = new ArrayList<>();

        for (SignatureEntry entry : signatureData.getSignatures()) {
            args.add(new Object[]{
                    appId.getOrganisationId(),
                    appId.getAppId(),
                    appId.getJvmId(),
                    entry.getSignature(),
                    compensateForClockSkew(entry.getInvokedAtMillis(), agentClockSkewMillis),
                    entry.getMillisSinceJvmStart(),
                    entry.getConfidence() == null ? null : entry.getConfidence().ordinal()
            });
        }

        jdbcTemplate.batchUpdate(
                "MERGE INTO signatures(organisation_id, application_id, jvm_id, signature, invoked_at_millis, millis_since_jvm_start, " +
                        "confidence) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?)", args);

    }

    private long compensateForClockSkew(Long timestamp, long skewMillis) {
        // zero means "no timestamp". Don't destroy that...
        return timestamp == 0L ? timestamp : timestamp + skewMillis;
    }

    @Override
    public void storeJvmData(long organisationId, long appId, JvmData data) {
        // Calculate the agent clock skew. Don't try to compensate for network latency...
        long agentClockSkewMillis = calculateAgentClockSkewMillis(data.getDaemonTimeMillis());

        long startedAtMillis = compensateForClockSkew(data.getStartedAtMillis(), agentClockSkewMillis);
        long reportedAtMillis = compensateForClockSkew(data.getDumpedAtMillis(), agentClockSkewMillis);
        long nextReportExpectedBeforeMillis = reportedAtMillis + (data.getCollectorResolutionSeconds() + data
                .getDaemonUploadIntervalSeconds()) * 1000L;

        // Add some tolerance to avoid false negatives...
        nextReportExpectedBeforeMillis += 60_000L;

        int updated =
                jdbcTemplate
                        .update("UPDATE jvm_info SET reported_at_millis = ?," +
                                        "next_report_expected_before_millis = ?, " +
                                        "agent_clock_skew_millis = ? " +
                                        "WHERE application_id = ? AND jvm_uuid = ?",
                                reportedAtMillis, nextReportExpectedBeforeMillis, agentClockSkewMillis,
                                appId, data.getJvmUuid
                                        ());
        if (updated > 0) {
            log.debug("Updated JVM info for {} {}", data.getAppName(), data.getAppVersion());
            return;
        }

        updated = jdbcTemplate
                .update("INSERT INTO jvm_info(organisation_id, application_id, application_version, jvm_uuid, " +
                                "agent_computer_id, agent_host_name, agent_upload_interval_seconds, agent_vcs_id, agent_version, " +
                                "agent_clock_skew_millis, " +
                                "collector_computer_id, collector_host_name, collector_resolution_seconds, collector_vcs_id, " +
                                "collector_version, method_visibility, started_at_millis, reported_at_millis, " +
                                "next_report_expected_before_millis, tags)" +
                                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        organisationId, appId, data.getAppVersion(), data.getJvmUuid(), data.getDaemonComputerId(), data
                                .getDaemonHostName(),
                        data.getDaemonUploadIntervalSeconds(), data.getDaemonVcsId(), data.getDaemonVersion(), agentClockSkewMillis,
                        data.getCollectorComputerId(), data.getCollectorHostName(), data.getCollectorResolutionSeconds(),
                        data.getCollectorVcsId(),
                        data.getCollectorVersion(), data.getMethodVisibility(), startedAtMillis, reportedAtMillis,
                        nextReportExpectedBeforeMillis, normalizeTags(data.getTags()));

        if (updated == 1) {
            log.debug("Stored JVM info for {} {}", data.getAppName(), data.getAppVersion());
        } else {
            log.warn("Cannot store JVM info {}", data);
        }
    }

    private long calculateAgentClockSkewMillis(long daemonTimeMillis) {
        return daemonTimeMillis <= 0L ? 0L : System.currentTimeMillis() - daemonTimeMillis;
    }

    String normalizeTags(String tags) {
        String normalizedTags = tags == null ? null : tags.trim();
        return normalizedTags == null || normalizedTags.isEmpty() ? null : normalizedTags;
    }

    @Override
    public WebSocketMessage createWebSocketMessage(long organisationId) {

        return WebSocketMessage.builder()
                               .usernames(getInteractiveUsernamesInOrganisation(organisationId))
                               .applications(queryApplications(organisationId))
                               .applicationStatistics(queryApplicationStatistics(organisationId))
                .collectors(queryCollectors(organisationId))
                .environments(queryEnvironments(organisationId))
                .build();
    }

    private Collection<EnvironmentDisplay> queryEnvironments(long organisationId) {

        EnvironmentRowCallbackHandler callbackHandler = new EnvironmentRowCallbackHandler();

        jdbcTemplate.query("SELECT " +
                                   "e.name, " +
                                   "eh.host_name " +
                                   "FROM environments e, environment_hostnames eh " +
                                   "WHERE e.organisation_id = ? " +
                                   "AND eh.environment_id = e.id " +
                                   "ORDER BY e.name ",
                           callbackHandler, organisationId);
        callbackHandler.saveCurrent();
        return callbackHandler.getResult();
    }

    private Collection<CollectorDisplay> queryCollectors(long organisationId) {
        // select the latest jvm_info for each application/version/hostname combination
        return jdbcTemplate.query("SELECT " +
                                          "a.name, " +
                                          "jvm.application_version, " +
                                          "jvm.agent_host_name, " +
                                          "jvm.agent_version, " +
                                          "jvm.agent_vcs_id, " +
                                          "jvm.agent_upload_interval_seconds, " +
                                          "jvm.agent_clock_skew_millis, " +
                                          "jvm.collector_host_name, " +
                                          "jvm.collector_version, " +
                                          "jvm.collector_vcs_id, " +
                                          "jvm.collector_resolution_seconds, " +
                                          "jvm.method_visibility, " +
                                          "jvm.started_at_millis, " +
                                          "jvm.reported_at_millis " +
                                          "FROM applications a, jvm_info jvm " +
                                          "WHERE a.id = jvm.application_id " +
                                          "AND jvm.id = (" +
                                          "  SELECT MAX(jvm2.id) " +
                                          "  FROM jvm_info jvm2 " +
                                          "  WHERE jvm2.application_id = jvm.application_id " +
                                          "  AND jvm2.application_version = jvm.application_version " +
                                          "  AND jvm2.collector_host_name = jvm.collector_host_name" +
                                          ") " +
                                          "AND a.organisation_id = ? " +
                                          "ORDER BY jvm.id ",
                                  new CollectorDisplayRowMapper(), organisationId);
    }

    private Collection<ApplicationStatisticsDisplay> queryApplicationStatistics(long organisationId) {
        return jdbcTemplate.query("SELECT " +
                                          "a.name, " +
                                          "a.usage_cycle_seconds, " +
                                          "stat.application_version, " +
                                          "stat.num_host_names, " +
                                          "stat.num_signatures, " +
                                          "stat.num_not_invoked_signatures, " +
                                          "stat.num_invoked_signatures, " +
                                          "stat.num_bootstrap_signatures, " +
                                          "stat.num_possibly_dead_signatures, " +
                                          "stat.first_started_at_millis, " +
                                          "stat.last_reported_at_millis, " +
                                          "stat.sum_up_time_millis, " +
                                          "stat.avg_up_time_millis, " +
                                          "stat.min_up_time_millis, " +
                                          "stat.max_up_time_millis " +
                                          "FROM applications a, application_statistics stat " +
                                          "WHERE stat.application_id = a.id " +
                                          "AND a.organisation_id = ? ",
                                  new ApplicationStatisticsDisplayRowMapper(), organisationId);
    }

    private Collection<ApplicationDisplay> queryApplications(long organisationId) {
        return jdbcTemplate.query("SELECT " +
                                          "a.name, " +
                                          "a.usage_cycle_seconds " +
                                          "FROM applications a " +
                                          "WHERE a.organisation_id = ? ",
                                  new ApplicationDisplayRowMapper(), organisationId);
    }

    @Override
    public Collection<String> saveSettings(long organisationId, OrganisationSettings organisationSettings) {

        List<Object[]> args = new ArrayList<>();

        for (ApplicationSettingsEntry entry : organisationSettings.getApplicationSettings()) {
            args.add(new Object[]{
                    entry.getUsageCycleSeconds(),
                    organisationId,
                    entry.getName(),
                    entry.getUsageCycleSeconds()

            });
        }

        int[] updated = jdbcTemplate.batchUpdate("UPDATE applications SET usage_cycle_seconds = ? " +
                                                         "WHERE organisation_id = ? " +
                                                         "AND name = ? " +
                                                         "AND usage_cycle_seconds <> ? ", args);

        // TODO: save organisationSettings.environments

        Set<String> updatedApps = new HashSet<>();

        for (int i = 0; i < updated.length; i++) {
            int count = updated[i];
            if (count != 0) {
                updatedApps.add(organisationSettings.getApplicationSettings().get(i).getName());
            }
        }

        if (updatedApps.isEmpty()) {
            log.info("Nothing to save");
        } else {
            log.info("Saved settings for {}", updatedApps.stream().collect(Collectors.joining(", ")));
        }

        return updatedApps;
    }

    @Override
    public void recalculateApplicationStatistics(AppId appId) {
        long startedAt = System.currentTimeMillis();

        Map<String, Object> data = jdbcTemplate.queryForMap("SELECT a.name AS appName, " +
                                                                    "a.usage_cycle_seconds AS usageCycleSeconds, " +
                                                                    "MIN(jvm.started_at_millis) AS minStartedAtMillis, " +
                                                                    "MAX(jvm.started_at_millis) AS maxStartedAtMillis, " +
                                                                    "MAX(jvm.reported_at_millis) AS maxReportedAtMillis, " +
                                                                    "SUM(jvm.reported_at_millis - jvm.started_at_millis) AS sumUptime, " +
                                                                    "AVG(jvm.reported_at_millis - jvm.started_at_millis) AS avgUptime, " +
                                                                    "MIN(jvm.reported_at_millis - jvm.started_at_millis) AS minUptime, " +
                                                                    "MAX(jvm.reported_at_millis - jvm.started_at_millis) AS maxUptime " +
                                                                    "FROM applications a, jvm_info jvm " +
                                                                    "WHERE jvm.application_id = a.id " +
                                                                    "AND jvm.application_id = ? " +
                                                                    "AND jvm.application_version = ? " +
                                                                    "GROUP BY " +
                                                                    "jvm.application_id, " +
                                                                    "jvm.application_version",
                                                            appId.getAppId(), appId.getAppVersion());

        String appName = (String) data.get("appName");
        int usageCycleSeconds = (int) data.get("usageCycleSeconds");
        long minStartedAtMillis = (long) data.get("minStartedAtMillis");
        long maxStartedAtMillis = (long) data.get("maxStartedAtMillis");
        long maxReportedAtMillis = (long) data.get("maxReportedAtMillis");
        long sumUpTimeMillis = ((BigDecimal) data.get("sumUpTime")).longValue();
        long avgUpTimeMillis = (long) data.get("avgUpTime");
        long minUpTimeMillis = (long) data.get("minUpTime");
        long maxUpTimeMillis = (long) data.get("maxUpTime");
        long bootstrapIfInvokedBeforeMillis = maxStartedAtMillis + 60_000L;
        long possiblyDeadIfInvokedBeforeMillis = maxReportedAtMillis - (usageCycleSeconds * 1000L);

        int numHostNames = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT(collector_host_name)) FROM jvm_info jvm " +
                                                               "WHERE jvm.application_id = ? " +
                                                               "AND jvm.application_version = ? ",
                                                       Integer.class,
                                                       appId.getAppId(), appId.getAppVersion());

        int numSignatures = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM signatures s, jvm_info jvm " +
                                                                "WHERE s.jvm_id = jvm.id " +
                                                                "AND jvm.application_id = ? " +
                                                                "AND jvm.application_version = ? ",
                                                        Integer.class,
                                                        appId.getAppId(), appId.getAppVersion());

        int numInvokedSignatures = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM signatures s, jvm_info jvm " +
                                                                       "WHERE s.jvm_id = jvm.id " +
                                                                       "AND jvm.application_id = ? " +
                                                                       "AND jvm.application_version = ? " +
                                                                       "AND s.invoked_at_millis > 0 ",
                                                               Integer.class,
                                                               appId.getAppId(), appId.getAppVersion());

        int numBootstrapSignatures = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM signatures s, jvm_info jvm " +
                                                                         "WHERE s.jvm_id = jvm.id " +
                                                                         "AND jvm.application_id = ? " +
                                                                         "AND jvm.application_version = ? " +
                                                                         "AND s.invoked_at_millis BETWEEN ? AND ? ",
                                                                 Integer.class,
                                                                 appId.getAppId(), appId.getAppVersion(),
                                                                 maxStartedAtMillis, bootstrapIfInvokedBeforeMillis);

        int numPossiblyDeadSignatures = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM signatures s, jvm_info jvm " +
                                                                            "WHERE s.jvm_id = jvm.id " +
                                                                            "AND jvm.application_id = ? " +
                                                                            "AND jvm.application_version = ? " +
                                                                            "AND s.invoked_at_millis BETWEEN ? AND ? ",
                                                                    Integer.class,
                                                                    appId.getAppId(), appId.getAppVersion(),
                                                                    bootstrapIfInvokedBeforeMillis,
                                                                    possiblyDeadIfInvokedBeforeMillis);

        int numNeverInvokedSignatures = numSignatures - numInvokedSignatures;

        jdbcTemplate.update("MERGE INTO application_statistics(application_id, application_version, " +
                                    "num_host_names, num_signatures, num_not_invoked_signatures, num_invoked_signatures, " +
                                    "num_bootstrap_signatures, num_possibly_dead_signatures, " +
                                    "first_started_at_millis, max_started_at_millis, last_reported_at_millis, " +
                                    "sum_up_time_millis, avg_up_time_millis, min_up_time_millis, max_up_time_millis) " +
                                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            appId.getAppId(), appId.getAppVersion(), numHostNames,
                            numSignatures, numNeverInvokedSignatures, numInvokedSignatures, numBootstrapSignatures,
                            numPossiblyDeadSignatures, minStartedAtMillis, maxStartedAtMillis, maxReportedAtMillis,
                            sumUpTimeMillis, avgUpTimeMillis, minUpTimeMillis, maxUpTimeMillis);

        long elapsed = System.currentTimeMillis() - startedAt;
        log.debug("Statistics for {} {} calculated in {} ms", appName, appId.getAppVersion(), elapsed);
    }

    @Override
    public int getNumCollectors(long organisationId, String appName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(jvm.id) FROM jvm_info jvm, applications a " +
                                                   "WHERE jvm.application_id = a.id " +
                                                   "AND a.organisation_id = ? " +
                                                   "AND a.name = ? ",
                                           Integer.class, organisationId, appName);
    }

    @Override
    public int getNumCollectors(long organisationId, String appName, String appVersion) {
        return jdbcTemplate.queryForObject("SELECT COUNT(jvm.id) FROM jvm_info jvm, applications a " +
                                                   "WHERE jvm.application_id = a.id " +
                                                   "AND a.organisation_id = ? " +
                                                   "AND a.name = ? " +
                                                   "AND jvm.application_version = ? ",
                                           Integer.class, organisationId, appName, appVersion);
    }

    @Override
    public int deleteCollectors(long organisationId, String appName, String appVersion, String hostName) {
        int rowsDeleted = jdbcTemplate.update("DELETE FROM signatures " +
                                                      "WHERE jvm_id IN ( " +
                                                      "  SELECT jvm.id " +
                                                      "  FROM jvm_info jvm, applications a " +
                                                      "  WHERE jvm.application_id = a.id " +
                                                      "    AND jvm.organisation_id = ? " +
                                                      "    AND a.name = ? " +
                                                      "    AND jvm.application_version = ? " +
                                                      "    AND jvm.collector_host_name = ? ) ",
                                              organisationId, appName, appVersion, hostName);

        rowsDeleted += jdbcTemplate.update("DELETE FROM jvm_info " +
                                                   "WHERE organisation_id = ? " +
                                                   "  AND application_version = ? " +
                                                   "  AND collector_host_name = ? " +
                                                   "  AND application_id IN (" +
                                                   "    SELECT id FROM applications " +
                                                   "    WHERE organisation_id = ? " +
                                                   "      AND name = ? ) ",
                                           organisationId, appVersion, hostName, organisationId, appName);
        return rowsDeleted;
    }

    @Override
    public int deleteApplication(long organisationId, String appName) {
        int rowsDeleted = jdbcTemplate.update("DELETE FROM application_statistics " +
                                                      "WHERE application_id = " +
                                                      "(SELECT id FROM applications WHERE organisation_id = ? AND name = ?)",
                                              organisationId, appName);
        rowsDeleted += jdbcTemplate.update("DELETE FROM applications WHERE organisation_id = ? AND name = ? ",
                                           organisationId, appName);
        return rowsDeleted;
    }

    @Override
    public int deleteApplicationStatistics(long organisationId, String appName, String appVersion) {
        return jdbcTemplate.update("DELETE FROM application_statistics " +
                                           "WHERE application_id = " +
                                           "(SELECT id FROM applications WHERE organisation_id = ? AND name = ?) " +
                                           "AND application_version = ? ",
                                   organisationId, appName, appVersion);
    }

    private static class ApplicationStatisticsDisplayRowMapper implements RowMapper<ApplicationStatisticsDisplay> {
        @Override
        public ApplicationStatisticsDisplay mapRow(ResultSet rs, int rowNum) throws SQLException {
            String appName = rs.getString(1);
            int usageCycleSeconds = rs.getInt(2);
            String appVersion = rs.getString(3);
            int numHostNames = rs.getInt(4);
            int numSignatures = rs.getInt(5);
            int numNeverInvokedSignatures = rs.getInt(6);
            int numInvokedSignatures = rs.getInt(7);
            int numBootstrapSignatures = rs.getInt(8);
            int numPossiblyDead = rs.getInt(9);
            long firstStartedAtMillis = rs.getLong(10);
            long lastDataReceivedAtMillis = rs.getLong(11);
            long sumUpTimeMillis = rs.getLong(12);
            long avgUpTimeMillis = rs.getLong(13);

            Integer percentDeadSignatures = numSignatures == 0 ? null : Math.round(numNeverInvokedSignatures * 100f / numSignatures);
            Integer percentPossiblyDeadSignatures = numSignatures == 0 ? null : Math.round(numPossiblyDead * 100f / numSignatures);
            Integer percentLiveSignatures = numSignatures == 0 ? null : 100 - percentDeadSignatures - percentPossiblyDeadSignatures;

            long upTimeSeconds = Math.round(sumUpTimeMillis / numHostNames / 1000d);

            return ApplicationStatisticsDisplay.builder()
                                               .name(appName)
                                               .usageCycleSeconds(usageCycleSeconds)
                                               .version(appVersion)
                                               .numHostNames(numHostNames)
                                               .numSignatures(numSignatures)
                                               .numNeverInvokedSignatures(numNeverInvokedSignatures)
                                               .percentNeverInvokedSignatures(percentDeadSignatures)
                                               .numInvokedSignatures(numInvokedSignatures)
                                               .percentInvokedSignatures(percentLiveSignatures)
                                               .numBootstrapSignatures(numBootstrapSignatures)
                                               .numPossiblyDeadSignatures(numPossiblyDead)
                                               .firstDataReceivedAtMillis(firstStartedAtMillis)
                                               .lastDataReceivedAtMillis(lastDataReceivedAtMillis)
                                               .upTimeSeconds(upTimeSeconds)
                                               .percentPossiblyDeadSignatures(percentPossiblyDeadSignatures)
                                               .fullUsageCycleCompleted(upTimeSeconds >= usageCycleSeconds)
                                               .build();
        }
    }

    private static class ApplicationDisplayRowMapper implements RowMapper<ApplicationDisplay> {
        @Override
        public ApplicationDisplay mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ApplicationDisplay.builder()
                                     .name(rs.getString(1))
                                     .usageCycleSeconds(rs.getInt(2))
                                     .build();
        }
    }

    private static class CollectorDisplayRowMapper implements RowMapper<CollectorDisplay> {
        @Override
        public CollectorDisplay mapRow(ResultSet rs, int rowNum) throws SQLException {
            return CollectorDisplay.builder()
                                   .appName(rs.getString(1))
                                   .appVersion(rs.getString(2))
                                   .agentHostname(rs.getString(3))
                                   .daemonVersion(String.format("%s.%s", rs.getString(4), rs.getString(5)))
                                   .daemonUploadIntervalSeconds(rs.getInt(6))
                                   .agentClockSkewMillis(rs.getLong(7))
                                   .collectorHostname(rs.getString(8))
                                   .collectorVersion(String.format("%s.%s", rs.getString(9), rs.getString(10)))
                                   .collectorResolutionSeconds(rs.getInt(11))
                                   .methodVisibility(rs.getString(12))
                                   .collectorStartedAtMillis(rs.getLong(13))
                                   .dataReceivedAtMillis(rs.getLong(14))
                                   .build();
        }
    }

    private static class EnvironmentRowCallbackHandler implements RowCallbackHandler {
        private final List<EnvironmentDisplay> result = new ArrayList<>();

        private String currentName = null;
        private final Set<String> currentHostNames = new TreeSet<>();

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            String name = rs.getString(1);
            String hostName = rs.getString(2);

            if (!name.equals(currentName)) {
                saveCurrent();
                currentName = name;
            }
            currentHostNames.add(hostName);
        }

        private void saveCurrent() {
            if (currentName != null) {
                result.add(EnvironmentDisplay.builder().name(currentName).hostNames(currentHostNames).build());
                currentName = null;
                currentHostNames.clear();
            }
        }

        List<EnvironmentDisplay> getResult() {
            saveCurrent();
            return result;
        }
    }
}
