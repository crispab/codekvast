package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.event.display.ApplicationStatisticsDisplay;
import se.crisp.codekvast.server.codekvast_server.model.event.display.ApplicationStatisticsMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.CollectorDisplay;
import se.crisp.codekvast.server.codekvast_server.model.event.display.CollectorStatusMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.CollectorSettings;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.CollectorSettingsEntry;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    public long getAppId(long organisationId, String appName, String appVersion) throws UndefinedApplicationException {
        log.debug("Looking up app id for {}:{}", organisationId, appName);
        return doGetOrCreateApp(organisationId, appName, appVersion);
    }

    private Long doGetOrCreateApp(long organisationId, String appName, String appVersion)
            throws UndefinedApplicationException {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM applications WHERE organisation_id = ? AND name = ? ",
                                               Long.class, organisationId, appName);
        } catch (EmptyResultDataAccessException ignored) {
        }

        long appId = doInsertRow("INSERT INTO applications(organisation_id, name, usage_cycle_seconds) VALUES(?, ?, ?)",
                                 organisationId, appName, codekvastSettings.getDefaultTrulyDeadAfterSeconds());

        log.info("Created application {}: '{} {}'", appId, appName, appVersion);
        return appId;
    }

    @Override
    @Transactional(readOnly = true)
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

    private static class AppIdRowMapper implements RowMapper<AppId> {
        @Override
        public AppId mapRow(ResultSet rs, int rowNum) throws SQLException {
            return AppId.builder().jvmId(rs.getLong(1)).organisationId(rs.getLong(2)).appId(rs.getLong(3))
                        .appVersion(rs.getString(4)).build();
        }
    }


    @Override
    public SignatureData storeInvocationData(AppId appId, SignatureData signatureData) {

        List<Object[]> args = new ArrayList<>();

        for (SignatureEntry entry : signatureData.getSignatures()) {
            args.add(new Object[]{
                    appId.getOrganisationId(),
                    appId.getAppId(),
                    appId.getJvmId(),
                    entry.getSignature(),
                    entry.getInvokedAtMillis(),
                    entry.getMillisSinceJvmStart(),
                    entry.getConfidence() == null ? null : entry.getConfidence().ordinal()
            });
        }

        int[] updated = jdbcTemplate.batchUpdate(
                "MERGE INTO signatures(organisation_id, application_id, jvm_id, signature, invoked_at_millis, millis_since_jvm_start, " +
                        "confidence) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?)", args);

        // Now check what really made it into the table...
        List<SignatureEntry> result = new ArrayList<>();
        int i = 0;
        for (SignatureEntry entry : signatureData.getSignatures()) {
            if (updated[i] > 0) {
                result.add(entry);
            }
            i += 1;
        }
        return SignatureData.builder().jvmUuid(signatureData.getJvmUuid()).signatures(result).build();
    }

    @Override
    public void storeJvmData(long organisationId, long appId, JvmData data) {
        int updated =
                jdbcTemplate
                        .update("UPDATE jvm_info SET dumped_at_millis= ? WHERE application_id = ? AND jvm_uuid = ?",
                                data.getDumpedAtMillis(), appId, data.getJvmUuid());
        if (updated > 0) {
            log.debug("Updated JVM info for {} {}", data.getAppName(), data.getAppVersion());
            return;
        }

        updated = jdbcTemplate
                .update("INSERT INTO jvm_info(organisation_id, application_id, application_version, jvm_uuid, " +
                                "collector_resolution_seconds, method_visibility, " +
                                "collector_computer_id, collector_host_name, agent_computer_id, agent_host_name, " +
                                "agent_upload_interval_seconds, " +
                                "codekvast_version, codekvast_vcs_id, started_at_millis, dumped_at_millis)" +
                                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        organisationId, appId, data.getAppVersion(), data.getJvmUuid(),
                        data.getCollectorResolutionSeconds(), data.getMethodVisibility(),
                        data.getCollectorComputerId(), data.getCollectorHostName(),
                        data.getAgentComputerId(), data.getAgentHostName(), data.getAgentUploadIntervalSeconds(),
                        data.getCodekvastVersion(), data.getCodekvastVcsId(), data.getStartedAtMillis(), data.getDumpedAtMillis());

        if (updated == 1) {
            log.debug("Stored JVM info for {} {}", data.getAppName(), data.getAppVersion());
        } else {
            log.warn("Cannot store JVM info {}", data);
        }
    }

    @Override
    public CollectorStatusMessage createCollectorStatusMessage(long organisationId) {
        Collection<String> usernames = getInteractiveUsernamesInOrganisation(organisationId);

        Collection<CollectorDisplay> collectors =
                jdbcTemplate.query("SELECT " +
                                           "a.name, " +
                                           "jvm.application_version, " +
                                           "jvm.collector_host_name, " +
                                           "a.usage_cycle_seconds, " +
                                           "MIN(jvm.started_at_millis), MAX(jvm.dumped_at_millis) " +
                                           "FROM applications a, jvm_info jvm " +
                                           "WHERE a.id = jvm.application_id " +
                                           "AND a.organisation_id = ? " +
                                           "GROUP BY a.name, jvm.application_version, jvm.collector_host_name, a.usage_cycle_seconds ",
                                   new CollectorDisplayRowMapper(), organisationId);

        return CollectorStatusMessage.builder().collectors(collectors).usernames(usernames).build();
    }

    @Override
    public void saveCollectorSettings(long organisationId, CollectorSettings collectorSettings) {

        List<Object[]> args = new ArrayList<>();

        for (CollectorSettingsEntry entry : collectorSettings.getCollectorSettings()) {
            args.add(new Object[]{
                    entry.getUsageCycleSeconds(),
                    organisationId,
                    entry.getName()
            });
        }

        int[] updated = jdbcTemplate.batchUpdate("UPDATE applications SET usage_cycle_seconds = ? " +
                                                         "WHERE organisation_id = ? AND name = ?", args);

        boolean success = true;
        for (int count : updated) {
            if (count != 1) {
                success = false;
            }
        }

        if (success) {
            log.info("Saved collector settings");
        } else {
            log.warn("Failed to save collector settings {}", collectorSettings);
        }

    }

    @Override
    public void recalculateApplicationStatistics(long organisationId) {
        List<AppId> appIds = jdbcTemplate.query("SELECT id, organisation_id, application_id, application_version FROM jvm_info WHERE " +
                                                        "organisation_id = ?",
                                                new AppIdRowMapper(), organisationId);
        for (AppId appId : appIds) {
            recalculateApplicationStatistics(appId);
        }
    }

    @Override
    public void recalculateApplicationStatistics(AppId appId) {
        long startedAt = System.currentTimeMillis();

        long now = System.currentTimeMillis();
        Map<String, Object> data = jdbcTemplate.queryForMap("SELECT a.name as D1, " +
                                                                    "MAX(jvm.started_at_millis) as D2, " +
                                                                    "a.usage_cycle_seconds as D3 " +
                                                                    "FROM jvm_info jvm, applications a " +
                                                                    "WHERE jvm.application_id = a.id " +
                                                                    "AND jvm.application_id = ? " +
                                                                    "AND jvm.application_version = ? " +
                                                                    "GROUP BY jvm.application_id, jvm.application_version ",
                                                            appId.getAppId(), appId.getAppVersion());

        String appName = (String) data.get("D1");
        long versionLastStartedAtMillis = (long) data.get("D2");
        int usageCycleSeconds = (int) data.get("D3");

        long startupRelatedIfInvokedBeforeMillis = versionLastStartedAtMillis + 60000L;

        long trulyDeadIfInvokedBeforeMillis = now - (usageCycleSeconds * 1000L);

        int numSignatures = jdbcTemplate.queryForObject("SELECT count(1) FROM signatures s, jvm_info jvm " +
                                                                "WHERE s.jvm_id = jvm.id " +
                                                                "AND jvm.application_id = ? " +
                                                                "AND jvm.application_version = ? ",
                                                        Integer.class,
                                                        appId.getAppId(), appId.getAppVersion());

        int numInvokedSignatures = jdbcTemplate.queryForObject("SELECT count(1) FROM signatures s, jvm_info jvm " +
                                                                       "WHERE s.jvm_id = jvm.id " +
                                                                       "AND jvm.application_id = ? " +
                                                                       "AND jvm.application_version = ? " +
                                                                       "AND s.invoked_at_millis > 0 ",
                                                               Integer.class,
                                                               appId.getAppId(), appId.getAppVersion());

        int numStartupSignatures = jdbcTemplate.queryForObject("SELECT count(1) FROM signatures s, jvm_info jvm " +
                                                                       "WHERE s.jvm_id = jvm.id " +
                                                                       "AND jvm.application_id = ? " +
                                                                       "AND jvm.application_version = ? " +
                                                                       "AND s.invoked_at_millis >= ? " +
                                                                       "AND s.invoked_at_millis < ? ",
                                                               Integer.class,
                                                               appId.getAppId(), appId.getAppVersion(),
                                                               versionLastStartedAtMillis, startupRelatedIfInvokedBeforeMillis);

        int numSignaturesInvokedBeforeUsageCycle = jdbcTemplate.queryForObject("SELECT count(1) FROM signatures s, jvm_info jvm " +
                                                                         "WHERE s.jvm_id = jvm.id " +
                                                                         "AND jvm.application_id = ? " +
                                                                         "AND jvm.application_version = ? " +
                                                                         "AND s.invoked_at_millis >= ? " +
                                                                         "AND s.invoked_at_millis < ? ",
                                                                 Integer.class,
                                                                 appId.getAppId(), appId.getAppVersion(),
                                                                 startupRelatedIfInvokedBeforeMillis, trulyDeadIfInvokedBeforeMillis);

        int numNeverInvokedSignatures = numSignatures - numInvokedSignatures;
        int numTrulyDeadSignatures = numNeverInvokedSignatures + numSignaturesInvokedBeforeUsageCycle;

        int updated = jdbcTemplate.update("MERGE INTO application_statistics(application_id, application_version, " +
                                                  "num_signatures, num_not_invoked_signatures, num_invoked_signatures, " +
                                                  "num_startup_signatures, " +
                                                  "num_truly_dead_signatures) " +
                                                  "VALUES(?, ?, ?, ?, ?, ?, ?)",
                                          appId.getAppId(), appId.getAppVersion(),
                                          numSignatures, numNeverInvokedSignatures, numInvokedSignatures, numStartupSignatures,
                                          numTrulyDeadSignatures);

        long elapsed = System.currentTimeMillis() - startedAt;
        log.debug("Statistics for {} {} calculated in {} ms", appName, appId.getAppVersion(), elapsed);
    }

    @Override
    public ApplicationStatisticsMessage createApplicationStatisticsMessage(long organisationId) {
        Collection<String> usernames = getInteractiveUsernamesInOrganisation(organisationId);

        Collection<ApplicationStatisticsDisplay> appStats =
                jdbcTemplate.query("SELECT " +
                                           "a.name, " +
                                           "a.usage_cycle_seconds, " +
                                           "stat.application_version, " +
                                           "stat.num_signatures, " +
                                           "stat.num_not_invoked_signatures, " +
                                           "stat.num_invoked_signatures, " +
                                           "stat.num_startup_signatures, " +
                                           "stat.num_truly_dead_signatures, " +
                                           "MIN(jvm.started_at_millis), " +
                                           "MAX(jvm.dumped_at_millis) " +
                                           "FROM applications a, application_statistics stat, jvm_info jvm " +
                                           "WHERE stat.application_id = a.id " +
                                           "AND jvm.application_id = a.id " +
                                           "AND jvm.application_version = stat.application_version " +
                                           "AND a.organisation_id = ? " +
                                           "GROUP BY a.id ",
                                   new ApplicationStatisticsDisplayRowMapper(), organisationId);

        return ApplicationStatisticsMessage.builder()
                                           .usernames(usernames)
                                           .applications(appStats)
                                           .build();
    }

    private static class ApplicationStatisticsDisplayRowMapper implements RowMapper<ApplicationStatisticsDisplay> {
        @Override
        public ApplicationStatisticsDisplay mapRow(ResultSet rs, int rowNum) throws SQLException {
            int usageCycleSeconds = rs.getInt(2);
            long firstDataReceivedAtMillis = rs.getLong(9);
            long fullUsageCycleEndsAtMillis = firstDataReceivedAtMillis + usageCycleSeconds * 1000L;
            int numSignatures = rs.getInt(4);
            int numInvokedSignatures = rs.getInt(6);
            Integer numTrulyDead = rs.getInt(8);
            Integer percentDeadSignatures = numSignatures == 0 ? null : Math.round(numTrulyDead * 100f / numSignatures);
            Integer percentInvokedSignatures = numSignatures == 0 ? null : Math.round(numInvokedSignatures * 100f / numSignatures);
            Integer percentNeverInvokedSignatures = percentInvokedSignatures == null ? null : 100 - percentInvokedSignatures;

            if (fullUsageCycleEndsAtMillis > System.currentTimeMillis()) {
                numTrulyDead = null;
                percentDeadSignatures = null;
            }

            return ApplicationStatisticsDisplay.builder()
                                               .name(rs.getString(1))
                                               .usageCycleSeconds(usageCycleSeconds)
                                               .version(rs.getString(3))
                                               .numSignatures(numSignatures)
                                               .numNeverInvokedSignatures(rs.getInt(5))
                                               .percentNeverInvokedSignatures(percentNeverInvokedSignatures)
                                               .numInvokedSignatures(numInvokedSignatures)
                                               .percentInvokedSignatures(percentInvokedSignatures)
                                               .numStartupSignatures(rs.getInt(7))
                                               .numTrulyDeadSignatures(numTrulyDead)
                                               .firstDataReceivedAtMillis(firstDataReceivedAtMillis)
                                               .lastDataReceivedAtMillis(rs.getLong(10))
                                               .fullUsageCycleEndsAtMillis(fullUsageCycleEndsAtMillis)
                                               .percentTrulyDeadSignatures(percentDeadSignatures)
                                               .build();
        }
    }

    private static class CollectorDisplayRowMapper implements RowMapper<CollectorDisplay> {
        @Override
        public CollectorDisplay mapRow(ResultSet rs, int rowNum) throws SQLException {
            return CollectorDisplay.builder()
                                   .name(rs.getString(1))
                                   .version(rs.getString(2))
                                   .hostname(rs.getString(3))
                                   .usageCycleSeconds(rs.getInt(4))
                                   .startedAtMillis(rs.getLong(5))
                                   .dataReceivedAtMillis(rs.getLong(6))
                                   .build();
        }
    }
}
