package se.crisp.codekvast.daemon.impl.local_warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
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
import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    private final Map<String, Long> signatureToIdMap = new HashMap<String, Long>();

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
        try {
            Jvm jvm = jvmState.getJvm();
            String json = objectMapper.writeValueAsString(jvm);
            jdbcTemplate.update("MERGE INTO jvms(jvm_uuid, json_data) VALUES(?, ?)", jvm.getJvmUuid(), json);
            log.debug("Updated JVM {}", jvm.getJvmUuid());
            jvmState.setJvmDataProcessedAt(jvmState.getJvm().getDumpedAtMillis());
        } catch (JsonProcessingException e) {
            throw new DataProcessingException("Cannot convert JVM data to JSON", e);
        }
    }

    @Override
    protected void doProcessCodebase(long now, JvmState jvmState, CodeBase codeBase) {
        updateSignatureToIdMap();
        if (saveSignatures(jvmState, codeBase.getSignatures())) {
            updateSignatureToIdMap();
        }
        jvmState.setCodebaseProcessedAt(now);
    }

    private boolean saveSignatures(JvmState jvmState, Set<String> signatures) {
        boolean anyInsert = false;
        for (String signature : signatures) {
            Long id = signatureToIdMap.get(signature);
            if (id == null) {
                jdbcTemplate.update("INSERT INTO methods(signature) VALUES(?)", signature);
                doStoreInvocation(jvmState, -1L, signature, null);
                anyInsert = true;
            }
        }
        return anyInsert;
    }

    private void updateSignatureToIdMap() {
        signatureToIdMap.clear();

        jdbcTemplate.query("SELECT id, signature FROM methods", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                Long id = rs.getLong(1);
                String signature = rs.getString(2);
                signatureToIdMap.put(signature, id);
            }
        });
    }

    @Override
    protected void doStoreNormalizedSignature(JvmState jvmState, Invocation invocation, String normalizedSignature,
                                              SignatureConfidence confidence) {
        doStoreInvocation(jvmState, invocation.getInvokedAtMillis(), normalizedSignature, confidence.ordinal());
    }

    private void doStoreInvocation(JvmState jvmState, Long invokedAtMillis, String normalizedSignature, Integer confidence) {
        // TODO: make sure not to overwrite newer invocations
        Long methodId = getSignatureId(normalizedSignature);
        jdbcTemplate.update("MERGE INTO invocations(method_id, jvm_uuid, invoked_at_millis, confidence, exported_at_millis) " +
                                    "KEY(method_id, jvm_uuid) " +
                                    "VALUES(?, ?, ?, ?, ?) ",
                            methodId, jvmState.getJvm().getJvmUuid(), invokedAtMillis, confidence, -1L);
    }

    private Long getSignatureId(String signature) {
        Long id = signatureToIdMap.get(signature);
        if (id == null) {
            log.debug("Cache miss for {}, inserting...", signature);
            jdbcTemplate.update("INSERT INTO methods(signature) VALUES(?)", signature);
            id = jdbcTemplate.queryForObject("SELECT id FROM methods WHERE signature = ?", new String[]{signature}, Long.class);
            signatureToIdMap.put(signature, id);
        }
        return id;
    }

    @Override
    protected void doProcessUnprocessedSignatures(JvmState jvmState) {
        // Nothing to do here
    }

}
