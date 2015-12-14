/**
 * Copyright (c) 2015 Crisp AB
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
package se.crisp.codekvast.warehouse.file_import;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.agent.lib.model.ExportFileMetaInfo;
import se.crisp.codekvast.agent.lib.model.v1.JvmData;

import javax.inject.Inject;
import java.sql.*;
import java.util.List;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Service
@Slf4j
public class ImportServiceImpl implements ImportService {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public ImportServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isFileImported(ExportFileMetaInfo metaInfo) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_file_info WHERE uuid = ? ", Integer.class, metaInfo.getUuid()) > 0;
    }

    @Override
    public void recordFileAsImported(ExportFileMetaInfo metaInfo, ImportStatistics importStatistics) {
        jdbcTemplate
                .update("INSERT INTO import_file_info(uuid, fileSchemaVersion, fileName, fileLengthBytes, importTimeMillis, " +
                                "importedFromDaemonHostname, importedFromEnvironment) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        metaInfo.getUuid(), metaInfo.getSchemaVersion(),
                        importStatistics.getImportFile().getPath(), importStatistics.getImportFile().length(),
                        importStatistics.getProcessingTime().toMillis(), metaInfo.getDaemonHostname(), metaInfo.getEnvironment());
        log.info("Imported {} {}", metaInfo, importStatistics);
    }

    @Override
    public void saveApplication(Application application, ImportContext context) {
        context.putApplication(getCentralApplicationId(application), application);
    }

    @Override
    public void saveMethod(Method method, ImportContext context) {
        context.putMethod(getCentralMethodId(method), method);
    }

    @Override
    public void saveJvm(Jvm jvm, JvmData jvmData, ImportContext context) {
        context.putJvm(getCentralJvmId(jvm, jvmData), jvm);
    }

    @Override
    public void saveInvocation(Invocation invocation, ImportContext context) {
        long applicationId = context.getApplicationId(invocation.getLocalApplicationId());
        long methodId = context.getMethodId(invocation.getLocalMethodId());
        long jvmId = context.getJvmId(invocation.getLocalJvmId());
        Timestamp invokedAt = new Timestamp(invocation.getInvokedAtMillis());

        Timestamp oldInvokedAt =
                queryForTimestamp("SELECT invokedAtMillis FROM invocations WHERE applicationId = ? AND methodId = ? AND jvmId = ? ",
                                  applicationId, methodId, jvmId);

        if (oldInvokedAt == null) {
            jdbcTemplate.update("INSERT INTO invocations(applicationId, methodId, jvmId, invokedAtMillis, invocationCount, confidence) " +
                                        "VALUES(?, ?, ?, ?, ?, ?) ",
                                applicationId, methodId, jvmId, invokedAt.getTime(),
                                invocation.getInvocationCount(), invocation.getConfidence().name());
            log.trace("Inserted invocation {}:{}:{} {}", applicationId, methodId, jvmId, invokedAt);
        } else if (invokedAt.after(oldInvokedAt)) {
            jdbcTemplate.update("UPDATE invocations SET invokedAtMillis = ?, invocationCount = invocationCount + ?, confidence = ? " +
                                        "WHERE applicationId = ? AND methodId = ? AND jvmId = ? ",
                                invokedAt.getTime(), invocation.getInvocationCount(), invocation.getConfidence().name(),
                                applicationId, methodId, jvmId);
            log.trace("Updated invocation {}:{}:{} {}", applicationId, methodId, jvmId, invokedAt);
        } else if (oldInvokedAt.equals(invokedAt)) {
            log.trace("Ignoring invocation, same row exists in database");
        } else {
            log.trace("Ignoring invocation, a newer row exists in database");
        }
    }

    @Override
    public void beginInsert() {
        jdbcTemplate.execute("BEGIN");
    }

    @Override
    public void endInsert() {
        jdbcTemplate.execute("COMMIT");
    }

    private Long doInsertRow(PreparedStatementCreator psc) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(psc, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private long getCentralApplicationId(Application app) {
        Long appId = queryForLong("SELECT id FROM applications WHERE name = ? AND version = ? ",
                                  app.getName(), app.getVersion());

        if (appId == null) {
            appId = doInsertRow(new InsertApplicationStatement(app));
            log.debug("Stored application {}:{}", appId, app);
        }
        return appId;
    }

    private long getCentralMethodId(Method method) {
        Long methodId = queryForLong("SELECT id FROM methods WHERE signature = ? ", method.getSignature());

        if (methodId == null) {
            methodId = doInsertRow(new InsertMethodStatement(method));
            log.trace("Stored method {}:{}", methodId, method.getSignature());
        }
        return methodId;
    }

    private long getCentralJvmId(Jvm jvm, JvmData jvmData) {
        Long jvmId = queryForLong("SELECT id FROM jvms WHERE uuid = ? ", jvm.getUuid());

        if (jvmId == null) {
            jvmId = doInsertRow(new InsertJvmStatement(jvm, jvmData));
            log.trace("Stored JVM {}:{}", jvmId, jvm);
        }
        return jvmId;
    }

    @RequiredArgsConstructor
    private static class InsertApplicationStatement implements PreparedStatementCreator {
        private final Application app;

        @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps = con.prepareStatement("INSERT INTO applications(name, version, createdAt) " +
                                                                "VALUES(?, ?, ?)",
                                                        Statement.RETURN_GENERATED_KEYS);
            int column = 0;
            ps.setString(++column, app.getName());
            ps.setString(++column, app.getVersion());
            ps.setTimestamp(++column, new Timestamp(app.getCreatedAtMillis()));
            return ps;
        }
    }

    @RequiredArgsConstructor
    private static class InsertMethodStatement implements PreparedStatementCreator {
        private final Method method;

        @SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "Duplicates"})
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps = con.prepareStatement("INSERT INTO methods(visibility, signature, createdAt, declaringType, " +
                                                                "exceptionTypes, methodName, modifiers, packageName, parameterTypes, " +
                                                                "returnType) " +
                                                                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                                        Statement.RETURN_GENERATED_KEYS);
            int column = 0;
            ps.setString(++column, method.getVisibility());
            ps.setString(++column, method.getSignature());
            ps.setTimestamp(++column, new Timestamp(method.getCreatedAtMillis()));
            ps.setString(++column, method.getDeclaringType());
            ps.setString(++column, method.getExceptionTypes());
            ps.setString(++column, method.getMethodName());
            ps.setString(++column, method.getModifiers());
            ps.setString(++column, method.getPackageName());
            ps.setString(++column, method.getParameterTypes());
            ps.setString(++column, method.getReturnType());
            return ps;
        }
    }

    @RequiredArgsConstructor
    private static class InsertJvmStatement implements PreparedStatementCreator {
        private final Jvm jvm;
        private final JvmData jvmData;

        @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps = con.prepareStatement("INSERT INTO jvms(uuid, " +
                                                                "startedAt, " +
                                                                "dumpedAt, " +
                                                                "collectorResolutionSeconds, " +
                                                                "methodVisibility, " +
                                                                "packages, " +
                                                                "excludePackages, " +
                                                                "environment, " +
                                                                "collectorComputerId, " +
                                                                "collectorHostName, " +
                                                                "collectorVersion, " +
                                                                "collectorVcsId, " +
                                                                "tags) " +
                                                                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                                        Statement.RETURN_GENERATED_KEYS);
            int column = 0;
            ps.setString(++column, jvm.getUuid());
            ps.setTimestamp(++column, new Timestamp(jvm.getStartedAtMillis()));
            ps.setTimestamp(++column, new Timestamp(jvm.getDumpedAtMillis()));
            ps.setInt(++column, jvmData.getCollectorResolutionSeconds());
            ps.setString(++column, jvmData.getMethodVisibility());
            ps.setString(++column, jvmData.getPackages());
            ps.setString(++column, jvmData.getExcludePackages());
            ps.setString(++column, jvmData.getEnvironment());
            ps.setString(++column, jvmData.getCollectorComputerId());
            ps.setString(++column, jvmData.getCollectorHostName());
            ps.setString(++column, jvmData.getCollectorVersion());
            ps.setString(++column, jvmData.getCollectorVcsId());
            ps.setString(++column, jvmData.getTags());
            return ps;
        }
    }

    private Long queryForLong(String sql, Object... args) {
        List<Long> list = jdbcTemplate.queryForList(sql, Long.class, args);
        return list.isEmpty() ? null : list.get(0);
    }

    private Timestamp queryForTimestamp(String sql, Object... args) {
        List<Long> list = jdbcTemplate.queryForList(sql, Long.class, args);
        return list.isEmpty() ? null : new Timestamp(list.get(0));
    }
}
