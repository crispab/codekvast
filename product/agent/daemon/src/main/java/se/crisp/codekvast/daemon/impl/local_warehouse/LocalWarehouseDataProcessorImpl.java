package se.crisp.codekvast.daemon.impl.local_warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
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
import se.crisp.codekvast.daemon.impl.AbstractDataProcessorImpl;
import se.crisp.codekvast.daemon.impl.DataProcessingException;
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
 * An implementation of DataProcessor that stores invocation data in a local data warehouse.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Profile("localWarehouse")
@Slf4j
public class LocalWarehouseDataProcessorImpl extends AbstractDataProcessorImpl {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Inject
    public LocalWarehouseDataProcessorImpl(@Nonnull DaemonConfig config,
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
        long jvmId = storeJvm(jvm);

        jvmState.setProcessed(applicationId, jvmId, jvmState.getJvm().getDumpedAtMillis());
    }

    private long storeApplication(final String name, final String version, final long createdAtMillis) {
        Long appId = queryForLong("SELECT id FROM applications WHERE name = ? AND version = ? ", name, version);

        if (appId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new InsertApplication(name, version, createdAtMillis), keyHolder);
            appId = keyHolder.getKey().longValue();
            log.debug("Stored application {}:{}:{}", appId, name, version);
        }
        return appId;
    }

    private long storeJvm(final Jvm jvm) {
        Long jvmId = queryForLong("SELECT id FROM jvms WHERE uuid = ? ", jvm.getJvmUuid());

        if (jvmId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new InsertJvm(jvm), keyHolder);
            jvmId = keyHolder.getKey().longValue();
            log.debug("Stored JVM {}:{}", jvmId, jvm.getJvmUuid());
        } else {
            jdbcTemplate.update("UPDATE jvms SET dumpedAtMillis = ? WHERE id = ?", jvm.getDumpedAtMillis(), jvmId);
            log.debug("Updated JVM {}:{}", jvmId, jvm.getJvmUuid());
        }
        return jvmId;
    }

    @Override
    protected void doProcessCodebase(long now, JvmState jvmState, CodeBase codeBase) {
        for (String signature : codeBase.getSignatures()) {
            doStoreInvocation(jvmState, -1L, signature, null);
        }
        jvmState.setCodebaseProcessedAt(now);
    }

    @Override
    protected void doStoreNormalizedSignature(JvmState jvmState, long invokedAtMillis, String signature, SignatureConfidence confidence) {
        doStoreInvocation(jvmState, invokedAtMillis, signature, confidence.ordinal());
    }

    private void doStoreInvocation(JvmState jvmState, long invokedAtMillis, final String signature, Integer confidence) {
        long applicationId = jvmState.getDatabaseAppId();
        long methodId = getMethodId(signature);
        long jvmId = jvmState.getDatabaseJvmId();
        long initialCount = invokedAtMillis > 0 ? 1 : 0;
        String what = invokedAtMillis > 0 ? "invocation" : "signature";

        Long oldInvokedAtMillis =
                queryForLong("SELECT invokedAtMillis FROM invocations WHERE applicationId = ? AND methodId = ? AND jvmId = ? ",
                             applicationId, methodId, jvmId);

        if (oldInvokedAtMillis == null) {
            jdbcTemplate.update("INSERT INTO invocations(applicationId, methodId, jvmId, invokedAtMillis, invocationCount, " +
                                        "confidence, exportedAtMillis) " +
                                        "VALUES(?, ?, ?, ?, ?, ?, ?) ",
                                applicationId, methodId, jvmId, invokedAtMillis, initialCount, confidence, -1L);
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

    private long getMethodId(final String signature) {
        Long methodId = queryForLong("SELECT id FROM methods WHERE signature = ?", signature);

        if (methodId == null) {

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new InsertMethod(signature), keyHolder);
            methodId = keyHolder.getKey().longValue();
            log.debug("Inserted method {}:{}...", methodId, signature);
        }
        return methodId;
    }

    @Override
    protected void doProcessUnprocessedSignatures(JvmState jvmState) {
        // Nothing to do here
    }

    private Long queryForLong(String sql, Object... args) {
        List<Long> list = jdbcTemplate.queryForList(sql, Long.class, args);
        return list.isEmpty() ? null : list.get(0);
    }

    @Value
    private static class InsertApplication implements PreparedStatementCreator {
        private final String name;
        private final String version;
        private final long createdAtMillis;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps = con.prepareStatement("INSERT INTO applications(name, version, createdAtMillis) VALUES(?, ?, ?)");
            ps.setString(1, name);
            ps.setString(2, version);
            ps.setLong(3, createdAtMillis);
            return ps;
        }
    }

    @Value
    private static class InsertMethod implements PreparedStatementCreator {
        private final String signature;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps = con.prepareStatement("INSERT INTO methods(signature, createdAtMillis) VALUES(?, ?)");
            ps.setString(1, signature);
            ps.setLong(2, System.currentTimeMillis());
            return ps;
        }
    }

    @Value
    private class InsertJvm implements PreparedStatementCreator {
        private final Jvm jvm;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps =
                    con.prepareStatement("INSERT INTO jvms(uuid, startedAtMillis, dumpedAtMillis, jsonData) VALUES(?, ?, ?, ?)");
            ps.setString(1, jvm.getJvmUuid());
            ps.setLong(2, jvm.getStartedAtMillis());
            ps.setLong(3, jvm.getDumpedAtMillis());
            ps.setString(4, asJson(jvm));
            return ps;
        }

        private String asJson(Jvm jvm) throws DataProcessingException {
            try {
                String json = objectMapper.writeValueAsString(jvm);
                checkState(json.length() <= 2000, "JVM as JSON string is longer than 2000 characters");
                return json;
            } catch (JsonProcessingException e) {
                throw new DataProcessingException("Cannot convert JVM data to JSON", e);
            }
        }

    }
}
