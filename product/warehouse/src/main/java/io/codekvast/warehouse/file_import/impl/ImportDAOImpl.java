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
package io.codekvast.warehouse.file_import.impl;

import io.codekvast.agent.lib.model.v1.CodeBaseEntry;
import io.codekvast.agent.lib.model.v1.CommonPublicationData;
import io.codekvast.agent.lib.model.v1.MethodSignature;
import io.codekvast.agent.lib.model.v1.SignatureStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class ImportDAOImpl implements ImportDAO {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public ImportDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long importApplication(CommonPublicationData commonData) {
        String name = commonData.getAppName();
        String version = commonData.getAppVersion();
        Timestamp createAt = new Timestamp(commonData.getJvmStartedAtMillis());

        int updated = jdbcTemplate.update("UPDATE applications SET createdAt = LEAST(createdAt, ?) " +
                                              "WHERE name = ? AND version = ?", createAt, name, version);
        if (updated != 0) {
            log.debug("Updated application {} {}", name, version);
        } else {
            jdbcTemplate.update("INSERT INTO applications(name, version, createdAt) VALUES (?, ?, ?)",
                                name, version, createAt);
            log.debug("Inserted application {} {} {}", name, version, createAt);
        }

        Long result = jdbcTemplate.queryForObject("SELECT id FROM applications WHERE name = ? AND version = ?", Long.class, name, version);
        log.debug("application.id={}", result);
        return result;
    }

    @Override
    public long importJvm(CommonPublicationData data) {

        Timestamp dumpedAt = new Timestamp(data.getPublishedAtMillis());

        int updated = jdbcTemplate.update("UPDATE jvms SET dumpedAt = ? " +
                                              "WHERE uuid = ?",
                                          dumpedAt, data.getJvmUuid());
        if (updated != 0) {
            log.debug("Updated JVM {}", data.getJvmUuid());
        } else {
            jdbcTemplate.update(
                "INSERT INTO jvms(uuid, startedAt, dumpedAt, collectorResolutionSeconds, methodVisibility, packages, excludePackages, " +
                    "environment, collectorComputerId, collectorHostname, collectorVersion, collectorVcsId, tags) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                data.getJvmUuid(), new Timestamp(data.getJvmStartedAtMillis()), dumpedAt, 0, data.getMethodVisibility(),
                data.getPackages(), data.getExcludePackages(), data.getEnvironment(), data.getComputerId(),
                data.getHostName(), data.getCollectorVersion(), "vcsId", data.getTags());

            log.debug("Inserted jvm {} started at {}", data.getJvmUuid(), Instant.ofEpochMilli(data.getJvmStartedAtMillis()));
        }

        Long result = jdbcTemplate.queryForObject("SELECT id FROM jvms WHERE uuid = ?", Long.class, data.getJvmUuid());

        log.debug("jvm.id={}", result);
        return result;
    }

    @Override
    public void importMethods(long appId, long jvmId, long publishedAtMillis, Collection<CodeBaseEntry> entries) {
        Map<String, Long> existingMethods = getExistingMethods();
        Set<Long> existingInvocations = getExistingInvocations(appId, jvmId);

        importNewMethods(publishedAtMillis, entries, existingMethods);
        importMissingInvocations(appId, jvmId, entries, existingMethods, existingInvocations);
    }

    private void importMissingInvocations(long appId, long jvmId, Collection<CodeBaseEntry> entries, Map<String, Long> existingMethods,
                                          Set<Long> existingInvocations) {
        long startedAtMillis = System.currentTimeMillis();
        int importCount = 0;
        for (CodeBaseEntry entry : entries) {
            long methodId = existingMethods.get(entry.getSignature());
            if (!existingInvocations.contains(methodId)) {
                jdbcTemplate.update(new InsertInvocationStatement(appId, jvmId, methodId, entry.getSignatureStatus()));
                importCount += 1;
            }
        }
        log.debug("Imported {} invocations in {} ms", importCount, System.currentTimeMillis() - startedAtMillis);
    }

    private void importNewMethods(long publishedAtMillis, Collection<CodeBaseEntry> entries,
                                  Map<String, Long> existingMethods) {
        long startedAtMillis = System.currentTimeMillis();
        int importCount = 0;
        for (CodeBaseEntry entry : entries) {
            String signature = entry.getSignature();
            if (!existingMethods.containsKey(signature)) {
                existingMethods.put(signature, doInsertRow(new InsertMethodStatement(publishedAtMillis, entry)));
                importCount += 1;
            }
        }
        log.debug("Imported {} methods in {} ms", importCount, System.currentTimeMillis() - startedAtMillis);
    }

    private Map<String, Long> getExistingMethods() {
        Map<String, Long> result = new HashMap<>();
        jdbcTemplate.query("SELECT id, signature FROM methods",
                           rs -> { result.put(rs.getString(2), rs.getLong(1)); });
        return result;
    }

    private Set<Long> getExistingInvocations(long appId, long jvmId) {
        Set<Long> result = new HashSet<>();
        jdbcTemplate.query("SELECT methodId FROM invocations WHERE applicationId = ? AND jvmId = ?",
                           rs -> { result.add(rs.getLong(1)); },
                           appId, jvmId);
        return result;
    }

    private Long doInsertRow(PreparedStatementCreator psc) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(psc, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @RequiredArgsConstructor
    private static class InsertMethodStatement implements PreparedStatementCreator {
        private final long publishedAtMillis;
        private final CodeBaseEntry entry;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {

            PreparedStatement ps = con.prepareStatement("INSERT INTO methods(visibility, signature, createdAt, declaringType, " +
                                                            "exceptionTypes, methodName, modifiers, packageName, parameterTypes, " +
                                                            "returnType) " +
                                                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                                        Statement.RETURN_GENERATED_KEYS);
            int column = 0;
            MethodSignature method = entry.getMethodSignature();
            ps.setString(++column, entry.getVisibility());
            ps.setString(++column, entry.getSignature());
            ps.setTimestamp(++column, new Timestamp(publishedAtMillis));
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
    private class InsertInvocationStatement implements PreparedStatementCreator {
        private final long appId;
        private final long jvmId;
        private final long methodId;
        private final SignatureStatus status;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps = con.prepareStatement("INSERT INTO invocations(applicationId, jvmId, methodId, status, invokedAtMillis, invocationCount) " +
                                                            "VALUES(?, ?, ?, ?, ?, ?)",
                                                        Statement.RETURN_GENERATED_KEYS);
            int column = 0;
            ps.setLong(++column, appId);
            ps.setLong(++column, jvmId);
            ps.setLong(++column, methodId);
            ps.setString(++column, status.name());
            ps.setLong(++column, 0L);
            ps.setLong(++column, 0L);
            return ps;
        }
    }
}
