package se.crisp.codekvast.daemon.worker.local_warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.daemon.appversion.AppVersionResolver;
import se.crisp.codekvast.daemon.beans.DaemonConfig;
import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;
import se.crisp.codekvast.daemon.codebase.CodeBaseScanner;
import se.crisp.codekvast.daemon.worker.AbstractCollectorDataProcessorImpl;
import se.crisp.codekvast.daemon.worker.DataProcessingException;
import se.crisp.codekvast.server.daemon_api.model.v1.JvmData;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureConfidence;
import se.crisp.codekvast.shared.model.Jvm;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * An implementation of CollectorDataProcessor that stores collected data in a local data warehouse.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Profile("localWarehouse")
@Slf4j
public class LocalWarehouseCollectorDataProcessorImpl extends AbstractCollectorDataProcessorImpl {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Inject
    public LocalWarehouseCollectorDataProcessorImpl(@Nonnull DaemonConfig config,
                                                    @Nonnull AppVersionResolver appVersionResolver,
                                                    @Nonnull CodeBaseScanner codeBaseScanner,
                                                    @Nonnull JdbcTemplate jdbcTemplate,
                                                    @Nonnull ObjectMapper objectMapper) {
        super(config, appVersionResolver, codeBaseScanner);
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        log.info("{} created", getClass().getSimpleName());
    }

    @Override
    protected void doProcessJvmData(JvmState jvmState) throws DataProcessingException {
        Jvm jvm = jvmState.getJvm();

        long applicationId = storeApplication(jvm.getCollectorConfig().getAppName(), jvmState.getAppVersion(), jvm.getStartedAtMillis());
        long jvmId = storeJvm(jvmState);

        jvmState.setDatabaseAppId(applicationId);
        jvmState.setDatabaseJvmId(jvmId);
    }

    @Override
    protected void doProcessCodebase(JvmState jvmState, CodeBase codeBase) {
        for (String signature : codeBase.getSignatures()) {
            doStoreInvocation(jvmState, -1L, signature, null);
        }
    }

    @Override
    protected void doStoreNormalizedSignature(JvmState jvmState, long invokedAtMillis, String signature, SignatureConfidence confidence) {
        doStoreInvocation(jvmState, invokedAtMillis, signature, confidence.ordinal());
    }

    @Override
    protected void doProcessUnprocessedSignatures(JvmState jvmState) {
        // Nothing to do here
    }

    private void doStoreInvocation(JvmState jvmState, long invokedAtMillis, final String signature, Integer confidence) {
        long applicationId = jvmState.getDatabaseAppId();
        long methodId = getMethodId(signature);
        long jvmId = jvmState.getDatabaseJvmId();
        long initialInvocationCount = invokedAtMillis > 0 ? 1 : 0;
        String what = invokedAtMillis > 0 ? "invocation" : "signature";

        Long oldInvokedAtMillis =
                queryForLong("SELECT invokedAtMillis FROM invocations WHERE applicationId = ? AND methodId = ? AND jvmId = ? ",
                             applicationId, methodId, jvmId);

        if (oldInvokedAtMillis == null) {
            jdbcTemplate.update("INSERT INTO invocations(applicationId, methodId, jvmId, invokedAtMillis, invocationCount, " +
                                        "confidence, exportedAtMillis) " +
                                        "VALUES(?, ?, ?, ?, ?, ?, ?) ",
                                applicationId, methodId, jvmId, invokedAtMillis, initialInvocationCount, confidence, -1L);
            log.debug("Stored {} {}:{}:{}", what, applicationId, methodId, jvmId);
        } else if (invokedAtMillis > oldInvokedAtMillis) {
            jdbcTemplate
                    .update("UPDATE invocations SET invokedAtMillis = ?, invocationCount = invocationCount + 1, confidence = ?, " +
                                    "exportedAtMillis = ? " +
                                    "WHERE applicationId = ? AND methodId = ? AND jvmId = ? ",
                            invokedAtMillis, confidence, -1L, applicationId, methodId, jvmId);
            log.debug("Updated {} {}:{}:{}", what, applicationId, methodId, jvmId);
        } else if (invokedAtMillis == oldInvokedAtMillis) {
            log.trace("Ignoring invocation of {}, same row exists in database", signature);
        } else {
            log.trace("Ignoring invocation of {}, a newer row exists in database", signature);
        }
    }

    private long getMethodId(String signatureWithVisibility) {
        int spacePos = signatureWithVisibility.indexOf(' ');
        String visibility = signatureWithVisibility.substring(0, spacePos);
        String signature = signatureWithVisibility.substring(spacePos + 1);
        Long methodId = queryForLong("SELECT id FROM methods WHERE signature = ? ", signature);

        if (methodId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new InsertMethodStatement(visibility, signature), keyHolder);
            methodId = keyHolder.getKey().longValue();
            log.debug("Inserted method {}:{}...", methodId, signature);
        }
        return methodId;
    }

    private long storeApplication(final String name, final String version, final long createdAtMillis) {
        Long appId = queryForLong("SELECT id FROM applications WHERE name = ? AND version = ? ", name, version);

        if (appId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new InsertApplicationStatement(name, version, createdAtMillis), keyHolder);
            appId = keyHolder.getKey().longValue();
            log.debug("Stored application {}:{}:{}", appId, name, version);
        }
        return appId;
    }

    private long storeJvm(JvmState jvmState) throws DataProcessingException {
        Jvm jvm = jvmState.getJvm();
        Long jvmId = queryForLong("SELECT id FROM jvms WHERE uuid = ? ", jvm.getJvmUuid());

        if (jvmId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new InsertJvmStatement(jvm, toJson(createUploadJvmData(jvmState))), keyHolder);
            jvmId = keyHolder.getKey().longValue();
            log.debug("Stored JVM {}:{}", jvmId, jvm.getJvmUuid());
        } else {
            jdbcTemplate.update("UPDATE jvms SET dumpedAtMillis = ? WHERE id = ?", jvm.getDumpedAtMillis(), jvmId);
            log.debug("Updated JVM {}:{}", jvmId, jvm.getJvmUuid());
        }
        return jvmId;
    }

    private Long queryForLong(String sql, Object... args) {
        List<Long> list = jdbcTemplate.queryForList(sql, Long.class, args);
        return list.isEmpty() ? null : list.get(0);
    }

    private String toJson(JvmData jvmData) throws DataProcessingException {
        try {
            String json = objectMapper.writeValueAsString(jvmData);
            checkState(json.length() <= 2000, "JvmData as JSON string is longer than 2000 characters");
            log.debug("JSON length = {}", json.length());
            return json;
        } catch (JsonProcessingException e) {
            throw new DataProcessingException("Cannot convert JvmData to JSON", e);
        }
    }

    @RequiredArgsConstructor
    private static class InsertApplicationStatement implements PreparedStatementCreator {
        private final String name;
        private final String version;
        private final long createdAtMillis;

        @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps = con.prepareStatement("INSERT INTO applications(name, version, createdAtMillis) VALUES(?, ?, ?)");
            int column = 0;
            ps.setString(++column, name);
            ps.setString(++column, version);
            ps.setLong(++column, createdAtMillis);
            return ps;
        }
    }

    @RequiredArgsConstructor
    private static class InsertMethodStatement implements PreparedStatementCreator {
        private final String visibility;
        private final String signature;

        @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps =
                    con.prepareStatement("INSERT INTO methods(visibility, signature, createdAtMillis) VALUES(?, ?, ?)");
            int column = 0;
            ps.setString(++column, visibility);
            ps.setString(++column, signature);
            ps.setLong(++column, System.currentTimeMillis());
            return ps;
        }
    }

    @RequiredArgsConstructor
    private static class InsertJvmStatement implements PreparedStatementCreator {
        private final Jvm jvm;
        private final String jsonData;

        @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps =
                    con.prepareStatement("INSERT INTO jvms(uuid, startedAtMillis, dumpedAtMillis, jsonData) VALUES(?, ?, ?, ?)");
            int column = 0;
            ps.setString(++column, jvm.getJvmUuid());
            ps.setLong(++column, jvm.getStartedAtMillis());
            ps.setLong(++column, jvm.getDumpedAtMillis());
            ps.setString(++column, jsonData);
            return ps;
        }
    }
}
