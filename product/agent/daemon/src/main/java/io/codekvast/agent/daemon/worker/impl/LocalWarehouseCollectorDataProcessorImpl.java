/*
 * Copyright (c) 2015-2017 Crisp AB
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
package io.codekvast.agent.daemon.worker.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codekvast.agent.daemon.appversion.AppVersionResolver;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import io.codekvast.agent.daemon.beans.DaemonConfig;
import io.codekvast.agent.daemon.beans.JvmState;
import io.codekvast.agent.daemon.worker.DataProcessingException;
import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.model.v1.CodeBaseEntry;
import io.codekvast.agent.lib.codebase.CodeBaseScanner;
import io.codekvast.agent.lib.model.v1.legacy.Jvm;
import io.codekvast.agent.lib.model.v1.MethodSignature;
import io.codekvast.agent.lib.model.v1.SignatureStatus;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * An implementation of CollectorDataProcessor that stores collected data in a local data warehouse implemented as
 * a file-based H2 database.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class LocalWarehouseCollectorDataProcessorImpl extends AbstractCollectorDataProcessorImpl {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Inject
    public LocalWarehouseCollectorDataProcessorImpl(@NonNull DaemonConfig config,
                                                    @NonNull AppVersionResolver appVersionResolver,
                                                    @NonNull CodeBaseScanner codeBaseScanner,
                                                    @NonNull JdbcTemplate jdbcTemplate,
                                                    @NonNull ObjectMapper objectMapper) {
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
        for (CodeBaseEntry entry : codeBase.getEntries()) {
            doStoreInvocation(jvmState, 0L, entry.getNormalizedSignature(), entry.getSignatureStatus(), entry.getMethodSignature());
        }
    }

    @Override
    protected void doStoreNormalizedSignature(JvmState jvmState, String normalizedSignature, long invokedAtMillis,
                                              SignatureStatus status) {
        doStoreInvocation(jvmState, invokedAtMillis, normalizedSignature, status, null);
    }

    @Override
    protected void doProcessUnprocessedInvocations(JvmState jvmState) {
        // Nothing to do here
    }

    private void doStoreInvocation(JvmState jvmState, long invokedAtMillis, String normalizedSignature, SignatureStatus status,
                                   MethodSignature methodSignature) {
        long applicationId = jvmState.getDatabaseAppId();
        long methodId = getMethodId(normalizedSignature, methodSignature);
        long jvmId = jvmState.getDatabaseJvmId();
        long initialInvocationCount = invokedAtMillis > 0 ? 1 : 0;
        String what = invokedAtMillis > 0 ? "invocation" : "signature";

        Long oldInvokedAtMillis =
                queryForLong("SELECT invokedAtMillis FROM invocations WHERE applicationId = ? AND methodId = ? AND jvmId = ? ",
                             applicationId, methodId, jvmId);

        if (oldInvokedAtMillis == null) {
            jdbcTemplate.update("INSERT INTO invocations(applicationId, methodId, jvmId, invokedAtMillis, invocationCount, " +
                                        "status) " +
                                        "VALUES(?, ?, ?, ?, ?, ?) ",
                                applicationId, methodId, jvmId, invokedAtMillis, initialInvocationCount, status.ordinal());
            log.trace("Stored {} {}:{}:{} {}", what, applicationId, methodId, jvmId, invokedAtMillis);
        } else if (invokedAtMillis > oldInvokedAtMillis) {
            jdbcTemplate
                    .update("UPDATE invocations SET invokedAtMillis = ?, invocationCount = invocationCount + 1, status = ? " +
                                    "WHERE applicationId = ? AND methodId = ? AND jvmId = ? ",
                            invokedAtMillis, status.ordinal(), applicationId, methodId, jvmId);
            log.trace("Updated {} {}:{}:{} {}", what, applicationId, methodId, jvmId, invokedAtMillis);
        } else if (invokedAtMillis == oldInvokedAtMillis) {
            log.trace("Ignoring invocation of {}, same row exists in database", normalizedSignature);
        } else {
            log.trace("Ignoring invocation of {}, a newer row exists in database", normalizedSignature);
        }
    }

    private long getMethodId(String normalizedSignature, MethodSignature methodSignature) {
        int spacePos = normalizedSignature.indexOf(' ');
        String visibility = normalizedSignature.substring(0, spacePos);
        String signature = normalizedSignature.substring(spacePos + 1);
        Long methodId = queryForLong("SELECT id FROM methods WHERE signature = ? ", signature);

        if (methodId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new InsertMethodStatement(visibility, signature, methodSignature), keyHolder);
            methodId = keyHolder.getKey().longValue();
            log.trace("Inserted method {}:{}...", methodId, signature);
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
            jdbcTemplate.update(new InsertJvmStatement(jvm, toJson(createJvmData(jvmState))), keyHolder);
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

    private String toJson(Object object) throws DataProcessingException {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new DataProcessingException("Cannot convert to JSON", e);
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
        private final MethodSignature methodSignature;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return methodSignature == null ? createInsertThinMethodStatement(con) : createInsertFatMethodStatement(con);
        }

        @NonNull
        @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
        private PreparedStatement createInsertThinMethodStatement(Connection con) throws SQLException {
            PreparedStatement ps =
                    con.prepareStatement("INSERT INTO methods(visibility, signature, createdAtMillis) VALUES(?, ?, ?)");
            int column = 0;
            ps.setString(++column, visibility);
            ps.setString(++column, signature);
            ps.setLong(++column, System.currentTimeMillis());
            return ps;
        }

        @NonNull
        @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
        private PreparedStatement createInsertFatMethodStatement(Connection con) throws SQLException {
            PreparedStatement ps =
                    con.prepareStatement(
                            "INSERT INTO methods(visibility, signature, createdAtMillis, declaringType, exceptionTypes, " +
                                    "methodName, modifiers, packageName, parameterTypes,returnType) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int column = 0;
            ps.setString(++column, visibility);
            ps.setString(++column, signature);
            ps.setLong(++column, System.currentTimeMillis());
            ps.setString(++column, methodSignature.getDeclaringType());
            ps.setString(++column, methodSignature.getExceptionTypes());
            ps.setString(++column, methodSignature.getMethodName());
            ps.setString(++column, methodSignature.getModifiers());
            ps.setString(++column, methodSignature.getPackageName());
            ps.setString(++column, methodSignature.getParameterTypes());
            ps.setString(++column, methodSignature.getReturnType());
            return ps;
        }
    }

    @RequiredArgsConstructor
    private static class InsertJvmStatement implements PreparedStatementCreator {
        private final Jvm jvm;
        private final String jvmDataJson;

        @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps =
                    con.prepareStatement("INSERT INTO jvms(uuid, startedAtMillis, dumpedAtMillis, jvmDataJson) VALUES(?, ?, ?, ?)");
            int column = 0;
            ps.setString(++column, jvm.getJvmUuid());
            ps.setLong(++column, jvm.getStartedAtMillis());
            ps.setLong(++column, jvm.getDumpedAtMillis());
            ps.setString(++column, jvmDataJson);
            return ps;
        }
    }
}
