package se.crisp.codekvast.daemon.impl.local_warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
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
import se.crisp.codekvast.shared.model.Invocation;
import se.crisp.codekvast.shared.model.Jvm;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

    @Nonnull
    private final JdbcTemplate jdbcTemplate;

    @Nonnull
    private final ObjectMapper objectMapper;

    private final Map<String, Long> methodIdBySignatureMap = new HashMap<String, Long>();
    private final Map<String, Map<Long, Long>> invokedAtMillisMapByJvmUuidMap = new HashMap<String, Map<Long, Long>>();

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

    @PostConstruct
    void populateCache() {
        methodIdBySignatureMap.clear();
        invokedAtMillisMapByJvmUuidMap.clear();

        jdbcTemplate.query("SELECT m.signature, i.method_id, i.jvm_uuid, i.invoked_at_millis " +
                                   "FROM methods m, invocations i " +
                                   "WHERE m.id = i.method_id ", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                String signature = rs.getString(1);
                long methodId = rs.getLong(2);
                String jvmUuid = rs.getString(3);
                long invokedAtMillis = rs.getLong(4);

                methodIdBySignatureMap.put(signature, methodId);

                Map<Long, Long> invokedAtMillisMap = getInvokedAtMillisMap(jvmUuid);
                invokedAtMillisMap.put(methodId, invokedAtMillis);
            }
        });
    }

    @Override
    protected void doProcessJvmData(JvmState jvmState) throws DataProcessingException {
        try {
            Jvm jvm = jvmState.getJvm();
            String json = objectMapper.writeValueAsString(jvm);
            checkState(json.length() <= 2000, "JSON string longer than 2000 characters");

            jdbcTemplate.update("MERGE INTO jvms(jvm_uuid, json_data) VALUES(?, ?)", jvm.getJvmUuid(), json);
            log.debug("Updated JVM {}", jvm.getJvmUuid());
            jvmState.setJvmDataProcessedAt(jvmState.getJvm().getDumpedAtMillis());
        } catch (JsonProcessingException e) {
            throw new DataProcessingException("Cannot convert JVM data to JSON", e);
        }
    }

    @Override
    protected void doProcessCodebase(long now, JvmState jvmState, CodeBase codeBase) {
        for (String signature : codeBase.getSignatures()) {
            doStoreInvocation(jvmState, -1L, signature, null);
        }
        jvmState.setCodebaseProcessedAt(now);
    }

    @Override
    protected void doStoreNormalizedSignature(JvmState jvmState, Invocation invocation, String normalizedSignature,
                                              SignatureConfidence confidence) {
        doStoreInvocation(jvmState, invocation.getInvokedAtMillis(), normalizedSignature, confidence.ordinal());
    }

    private void doStoreInvocation(JvmState jvmState, long invokedAtMillis, final String normalizedSignature, Integer confidence) {
        Long methodId = getMethodId(normalizedSignature);

        String jvmUuid = jvmState.getJvm().getJvmUuid();

        Map<Long, Long> invokedAtMillisMap = getInvokedAtMillisMap(jvmUuid);
        Long oldInvokedAtMillis = invokedAtMillisMap.get(methodId);

        if (oldInvokedAtMillis == null || invokedAtMillis > oldInvokedAtMillis) {
            jdbcTemplate.update("MERGE INTO invocations(method_id, jvm_uuid, invoked_at_millis, confidence, exported_at_millis) " +
                                        "KEY(method_id, jvm_uuid) " +
                                        "VALUES(?, ?, ?, ?, ?) ",
                                methodId, jvmUuid, invokedAtMillis, confidence, -1L);
            invokedAtMillisMap.put(methodId, invokedAtMillis);
        }
    }

    @Nonnull
    private Long getMethodId(final String signature) {
        Long methodId = methodIdBySignatureMap.get(signature);

        if (methodId == null) {

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO methods(signature) VALUES(?)");
                    ps.setString(1, signature);
                    return ps;
                }
            }, keyHolder);

            methodId = keyHolder.getKey().longValue();
            methodIdBySignatureMap.put(signature, methodId);
            log.debug("Inserted {}:{}...", methodId, signature);
        }
        return methodId;
    }

    @Nonnull
    private Map<Long, Long> getInvokedAtMillisMap(String jvmUuid) {
        Map<Long, Long> result = invokedAtMillisMapByJvmUuidMap.get(jvmUuid);
        if (result == null) {
            result = new HashMap<Long, Long>();
            invokedAtMillisMapByJvmUuidMap.put(jvmUuid, result);
        }
        return result;
    }

    @Override
    protected void doProcessUnprocessedSignatures(JvmState jvmState) {
        // Nothing to do here
    }

}
