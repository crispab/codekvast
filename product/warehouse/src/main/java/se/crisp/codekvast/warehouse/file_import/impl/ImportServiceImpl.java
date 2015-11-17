package se.crisp.codekvast.warehouse.file_import.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.agent.lib.model.ExportFileMetaInfo;
import se.crisp.codekvast.warehouse.file_import.Application;
import se.crisp.codekvast.warehouse.file_import.ImportService;
import se.crisp.codekvast.warehouse.file_import.Jvm;
import se.crisp.codekvast.warehouse.file_import.Method;

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
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_meta_info WHERE uuid = ? ", Integer.class, metaInfo.getUuid()) > 0;
    }

    @Override
    public void recordFileAsImported(ExportFileMetaInfo metaInfo) {
        jdbcTemplate
                .update("INSERT INTO file_meta_info(uuid, fileSchemaVersion, fileName, fileLengthBytes, importedFromDaemonHostname) " +
                                "VALUES (?, ?, ?, ?, ?)",
                        metaInfo.getUuid(), metaInfo.getSchemaVersion(), metaInfo.getFileName(), metaInfo.getFileLengthBytes(),
                        metaInfo.getDaemonHostname());
        log.info("Imported {}", metaInfo);
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
    public void saveJvm(Jvm jvm, ImportContext context) {
        context.putJvm(getCentralJvmId(jvm), jvm);
    }

    private long getCentralApplicationId(Application app) {
        Long appId = queryForLong("SELECT id FROM applications WHERE name = ? AND version = ? ",
                                  app.getName(), app.getVersion());

        //noinspection Duplicates
        if (appId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new InsertApplicationStatement(app), keyHolder);
            appId = keyHolder.getKey().longValue();
            log.debug("Stored application {}:{}", appId, app);
        }
        return appId;
    }

    private long getCentralMethodId(Method method) {
        Long methodId = queryForLong("SELECT id FROM methods WHERE signature = ? ", method.getSignature());

        //noinspection Duplicates
        if (methodId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new InsertMethodStatement(method), keyHolder);
            methodId = keyHolder.getKey().longValue();
            log.debug("Stored method {}:{}", methodId, method.getSignature());
        }
        return methodId;
    }

    private long getCentralJvmId(Jvm jvm) {
        Long jvmId = queryForLong("SELECT id FROM jvms WHERE uuid = ? ", jvm.getUuid());

        //noinspection Duplicates
        if (jvmId == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new InsertJvmStatement(jvm), keyHolder);
            jvmId = keyHolder.getKey().longValue();
            log.debug("Stored JVM {}:{}", jvmId, jvm);
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

        @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps = con.prepareStatement("INSERT INTO jvms(uuid, startedAt, dumpedAt, jvmDataJson) " +
                                                                "VALUES(?, ?, ?, ?)",
                                                        Statement.RETURN_GENERATED_KEYS);
            int column = 0;
            ps.setString(++column, jvm.getUuid());
            ps.setTimestamp(++column, new Timestamp(jvm.getStartedAtMillis()));
            ps.setTimestamp(++column, new Timestamp(jvm.getDumpedAtMillis()));
            ps.setString(++column, jvm.getJvmDataJson());
            return ps;
        }
    }

    private Long queryForLong(String sql, Object... args) {
        List<Long> list = jdbcTemplate.queryForList(sql, Long.class, args);
        return list.isEmpty() ? null : list.get(0);
    }
}
