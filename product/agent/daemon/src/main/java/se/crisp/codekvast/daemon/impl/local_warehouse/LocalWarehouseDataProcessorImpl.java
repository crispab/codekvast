package se.crisp.codekvast.daemon.impl.local_warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
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

    @Value
    @Builder
    private static class SignatureData {
        long id;

        @Wither
        long invokedAtMillis;
    }

    private final Map<String, SignatureData> signatureDataMap = new HashMap<String, SignatureData>();

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
    void populateSignatureDataMap() {
        signatureDataMap.clear();

        jdbcTemplate.query("SELECT m.signature, m.id, i.invoked_at_millis FROM methods m, invocations i " +
                                   "WHERE m.id = i.method_id ", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                String signature = rs.getString(1);
                SignatureData signatureData = SignatureData.builder().id(rs.getLong(2)).invokedAtMillis(rs.getLong(3)).build();
                SignatureData existing = signatureDataMap.get(signature);

                // Only save the latest invocation...
                if (existing == null || existing.getInvokedAtMillis() < signatureData.getInvokedAtMillis()) {
                    signatureDataMap.put(signature, signatureData);
                }
            }
        });
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
        SignatureData signatureData = signatureDataMap.get(normalizedSignature);

        if (signatureData == null) {
            log.debug("Inserting {}...", normalizedSignature);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO methods(signature) VALUES(?)");
                    ps.setString(1, normalizedSignature);
                    return ps;
                }
            }, keyHolder);

            signatureData = SignatureData.builder()
                                         .id(keyHolder.getKey().longValue())
                                         .invokedAtMillis(Long.MIN_VALUE)
                                         .build();

            signatureDataMap.put(normalizedSignature, signatureData);
        }


        if (invokedAtMillis > signatureData.getInvokedAtMillis()) {
            jdbcTemplate.update("MERGE INTO invocations(method_id, jvm_uuid, invoked_at_millis, confidence, exported_at_millis) " +
                                        "KEY(method_id, jvm_uuid) " +
                                        "VALUES(?, ?, ?, ?, ?) ",
                                signatureData.getId(), jvmState.getJvm().getJvmUuid(), invokedAtMillis, confidence, -1L);
            signatureDataMap.put(normalizedSignature, signatureData.withInvokedAtMillis(invokedAtMillis));
        }
    }

    @Override
    protected void doProcessUnprocessedSignatures(JvmState jvmState) {
        // Nothing to do here
    }

}
