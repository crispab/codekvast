/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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

import io.codekvast.javaagent.model.v1.CodeBaseEntry;
import io.codekvast.javaagent.model.v1.CommonPublicationData;
import io.codekvast.javaagent.model.v1.MethodSignature;
import io.codekvast.javaagent.model.v1.SignatureStatus;
import io.codekvast.warehouse.customer.CustomerService;
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
    private final CustomerService customerService;

    @Inject
    public ImportDAOImpl(JdbcTemplate jdbcTemplate, CustomerService customerService) {
        this.jdbcTemplate = jdbcTemplate;
        this.customerService = customerService;
    }

    @Override
    public long importApplication(CommonPublicationData data) {
        long customerId = data.getCustomerId();
        String name = data.getAppName();
        String version = data.getAppVersion();
        Timestamp createAt = new Timestamp(data.getJvmStartedAtMillis());

        int updated = jdbcTemplate.update("UPDATE applications SET createdAt = LEAST(createdAt, ?) " +
                                              "WHERE customerId = ? AND name = ? AND version = ?", createAt, customerId, name, version);
        if (updated != 0) {
            log.trace("Updated application {} {}", name, version);
        } else {
            jdbcTemplate.update("INSERT INTO applications(customerId, name, version, createdAt) VALUES (?, ?, ?, ?)",
                                customerId, name, version, createAt);
            log.trace("Inserted application {} {} {} {}", customerId, name, version, createAt);
        }

        Long result = jdbcTemplate
            .queryForObject("SELECT id FROM applications WHERE customerId = ? AND name = ? AND version = ?", Long.class, customerId, name,
                            version);
        log.debug("application {} {} {} has id {}", customerId, name, version, result);
        return result;
    }

    @Override
    public long importJvm(CommonPublicationData data, long applicationId) {

        long customerId = data.getCustomerId();
        Timestamp publishedAt = new Timestamp(data.getPublishedAtMillis());

        int updated = jdbcTemplate.update("UPDATE jvms SET publishedAt = ? WHERE uuid = ?",
                                          publishedAt, data.getJvmUuid());
        if (updated != 0) {
            log.trace("Updated JVM {}", data.getJvmUuid());
        } else {
            jdbcTemplate.update(
                "INSERT INTO jvms(customerId, applicationId, uuid, startedAt, publishedAt, methodVisibility, packages, excludePackages, " +
                    "environment, computerId, hostname, agentVersion, tags) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                customerId, applicationId, data.getJvmUuid(), new Timestamp(data.getJvmStartedAtMillis()), publishedAt,
                data.getMethodVisibility(),
                data.getPackages(), data.getExcludePackages(), data.getEnvironment(), data.getComputerId(), data.getHostname(),
                data.getAgentVersion(), data.getTags());
            log.trace("Inserted jvm {} {} started at {}", customerId, data.getJvmUuid(),
                      Instant.ofEpochMilli(data.getJvmStartedAtMillis()));
        }

        Long result = jdbcTemplate.queryForObject("SELECT id FROM jvms WHERE uuid = ?", Long.class, data.getJvmUuid());

        log.debug("JVM with uuid {} has id {}", data.getJvmUuid(), result);
        return result;
    }

    @Override
    public void importMethods(long customerId, long appId, long jvmId, long publishedAtMillis, Collection<CodeBaseEntry> entries) {
        Map<String, Long> existingMethods = getExistingMethods(customerId);
        Set<String> incompleteMethods = getIncompleteMethods(customerId);
        Set<Long> invocationsNotFoundInCodeBase = getInvocationsNotFoundInCodeBase(customerId);
        Set<Long> existingInvocations = getExistingInvocations(customerId, appId, jvmId);

        importNewMethods(customerId, publishedAtMillis, entries, existingMethods);
        updateIncompleteMethods(customerId, publishedAtMillis, entries, incompleteMethods, existingMethods, invocationsNotFoundInCodeBase);
        ensureInitialInvocations(customerId, appId, jvmId, entries, existingMethods, existingInvocations);
        // TODO: update initial invocation status if different from entries*.status

        customerService.assertDatabaseSize(customerId);
    }

    @Override
    public void importInvocations(long customerId, long appId, long jvmId, long invokedAtMillis, Set<String> invocations) {
        Map<String, Long> existingMethods = getExistingMethods(customerId);
        Set<Long> existingInvocations = getExistingInvocations(customerId, appId, jvmId);

        doImportInvocations(customerId, appId, jvmId, invokedAtMillis, invocations, existingMethods, existingInvocations);

        customerService.assertDatabaseSize(customerId);
    }

    private void doImportInvocations(long customerId, long appId, long jvmId, long invokedAtMillis, Set<String> invokedSignatures,
                                     Map<String, Long> existingMethods, Set<Long> existingInvocations) {
        for (String signature : invokedSignatures) {
            Long methodId = existingMethods.get(signature);
            if (methodId == null) {
                log.trace("Inserting incomplete method {}:{}", methodId, signature);
                existingMethods.put(signature, doInsertRow(new InsertIncompleteMethodStatement(customerId, signature, invokedAtMillis)));
                methodId = existingMethods.get(signature);
            }
            if (existingInvocations.contains(methodId)) {
                log.trace("Updating invocation {}", signature);
                jdbcTemplate.update(new UpdateInvocationStatement(customerId, appId, jvmId, methodId, invokedAtMillis));
            } else {
                log.trace("Inserting invocation {}", signature);
                jdbcTemplate
                    .update(new InsertInvocationStatement(customerId, appId, jvmId, methodId, SignatureStatus.NOT_FOUND_IN_CODE_BASE,
                                                          invokedAtMillis, 1L));
            }
        }
    }

    private Map<String, Long> getExistingMethods(long customerId) {
        Map<String, Long> result = new HashMap<>();
        jdbcTemplate.query("SELECT id, signature FROM methods WHERE customerId = " + customerId,
                           rs -> {
                               result.put(rs.getString(2), rs.getLong(1));
                           });
        return result;
    }

    private Set<String> getIncompleteMethods(long customerId) {
        return new HashSet<>(
            jdbcTemplate.queryForList("SELECT signature FROM methods WHERE methods.customerId = ? AND methodName IS NULL ", String.class,
                                      customerId));
    }

    private Set<Long> getInvocationsNotFoundInCodeBase(long customerId) {
        return new HashSet<>(
            jdbcTemplate
                .queryForList("SELECT methodId FROM invocations WHERE customerId = ? AND status = ?", Long.class,
                              customerId, SignatureStatus.NOT_FOUND_IN_CODE_BASE.name())
        );
    }


    private Set<Long> getExistingInvocations(long customerId, long appId, long jvmId) {
        return new HashSet<>(
            jdbcTemplate
                .queryForList("SELECT methodId FROM invocations WHERE customerId = ? AND applicationId = ? AND jvmId = ?", Long.class,
                              customerId, appId, jvmId));
    }

    private void importNewMethods(long customerId, long publishedAtMillis, Collection<CodeBaseEntry> entries,
                                  Map<String, Long> existingMethods) {
        long startedAtMillis = System.currentTimeMillis();
        int count = 0;
        for (CodeBaseEntry entry : entries) {
            String signature = entry.getSignature();
            if (!existingMethods.containsKey(signature)) {
                existingMethods.put(signature, doInsertRow(new InsertCompleteMethodStatement(customerId, publishedAtMillis, entry)));
                count += 1;
            }
        }
        log.debug("Imported {} methods in {} ms", count, System.currentTimeMillis() - startedAtMillis);
    }

    private void updateIncompleteMethods(long customerId, long publishedAtMillis, Collection<CodeBaseEntry> entries,
                                         Set<String> incompleteMethods,
                                         Map<String, Long> existingMethods, Set<Long> incompleteInvocations) {
        long startedAtMillis = System.currentTimeMillis();
        int count = 0;
        for (CodeBaseEntry entry : entries) {
            long methodId = existingMethods.get(entry.getSignature());
            if (incompleteMethods.contains(entry.getSignature()) || incompleteInvocations.contains(methodId)) {
                log.debug("Updating {}", entry.getSignature());
                jdbcTemplate.update(new UpdateIncompleteMethodStatement(customerId, publishedAtMillis, entry));
                count += 1;
            }
        }
        log.debug("Updated {} incomplete methods in {} ms", count, System.currentTimeMillis() - startedAtMillis);
    }

    private void ensureInitialInvocations(long customerId, long appId, long jvmId, Collection<CodeBaseEntry> entries,
                                          Map<String, Long> existingMethods, Set<Long> existingInvocations) {
        long startedAtMillis = System.currentTimeMillis();
        int importCount = 0;

        for (CodeBaseEntry entry : entries) {
            long methodId = existingMethods.get(entry.getSignature());
            if (!existingInvocations.contains(methodId)) {
                jdbcTemplate.update(new InsertInvocationStatement(customerId, appId, jvmId, methodId, entry.getSignatureStatus(),
                                                                  0L, 0L));
                existingInvocations.add(methodId);
                importCount += 1;
            }
        }
        log.debug("Imported {} invocations in {} ms", importCount, System.currentTimeMillis() - startedAtMillis);
    }

    private Long doInsertRow(PreparedStatementCreator psc) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(psc, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @RequiredArgsConstructor
    private static class InsertCompleteMethodStatement implements PreparedStatementCreator {
        private final long customerId;
        private final long publishedAtMillis;
        private final CodeBaseEntry entry;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {

            PreparedStatement ps =
                con.prepareStatement("INSERT INTO methods(customerId, visibility, signature, createdAt, declaringType, " +
                                         "exceptionTypes, methodName, modifiers, packageName, parameterTypes, " +
                                         "returnType) " +
                                         "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                     Statement.RETURN_GENERATED_KEYS);
            MethodSignature method = entry.getMethodSignature();
            int column = 0;
            ps.setLong(++column, customerId);
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
    private static class UpdateIncompleteMethodStatement implements PreparedStatementCreator {
        private final long customerId;
        private final long publishedAtMillis;
        private final CodeBaseEntry entry;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {

            PreparedStatement ps = con.prepareStatement(
                "UPDATE methods SET " +
                    "visibility = ?, createdAt = LEAST(createdAt, ?), declaringType = ?, exceptionTypes = ?, methodName = ?, modifiers = " +
                    "?" +
                    ", packageName = ?, parameterTypes = ?, returnType = ? WHERE customerId = ? AND signature = ?");
            int column = 0;
            MethodSignature method = entry.getMethodSignature();
            ps.setString(++column, entry.getVisibility());
            ps.setTimestamp(++column, new Timestamp(publishedAtMillis));
            ps.setString(++column, method.getDeclaringType());
            ps.setString(++column, method.getExceptionTypes());
            ps.setString(++column, method.getMethodName());
            ps.setString(++column, method.getModifiers());
            ps.setString(++column, method.getPackageName());
            ps.setString(++column, method.getParameterTypes());
            ps.setString(++column, method.getReturnType());
            ps.setLong(++column, customerId);
            ps.setString(++column, entry.getSignature());
            return ps;
        }
    }

    @RequiredArgsConstructor
    private class InsertInvocationStatement implements PreparedStatementCreator {
        private final long customerId;
        private final long appId;
        private final long jvmId;
        private final long methodId;
        private final SignatureStatus status;
        private final long invokedAtMillis;
        private final long invocationCount;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps =
                con.prepareStatement(
                    "INSERT INTO invocations(customerId, applicationId, jvmId, methodId, status, invokedAtMillis, invocationCount) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?)");
            int column = 0;
            ps.setLong(++column, customerId);
            ps.setLong(++column, appId);
            ps.setLong(++column, jvmId);
            ps.setLong(++column, methodId);
            ps.setString(++column, status.name());
            ps.setLong(++column, invokedAtMillis);
            ps.setLong(++column, invocationCount);
            return ps;
        }
    }

    @RequiredArgsConstructor
    private class UpdateInvocationStatement implements PreparedStatementCreator {
        private final long customerId;
        private final long appId;
        private final long jvmId;
        private final long methodId;
        private final long invokedAtMillis;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps =
                con.prepareStatement(
                    "UPDATE invocations SET invokedAtMillis = GREATEST(invokedAtMillis, ?), " +
                        "status = ?, invocationCount = invocationCount + 1 " +
                        "WHERE customerId = ? AND applicationId = ? AND jvmId = ? AND methodId = ?",
                    Statement.RETURN_GENERATED_KEYS);
            int column = 0;
            ps.setLong(++column, invokedAtMillis);
            ps.setString(++column, SignatureStatus.INVOKED.name());
            ps.setLong(++column, customerId);
            ps.setLong(++column, appId);
            ps.setLong(++column, jvmId);
            ps.setLong(++column, methodId);
            return ps;
        }
    }

    @RequiredArgsConstructor
    private class InsertIncompleteMethodStatement implements PreparedStatementCreator {
        private final long customerId;
        private final String signature;
        private final long invokedAtMillis;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps =
                con.prepareStatement("INSERT INTO methods(customerId, visibility, signature, createdAt) VALUES (?, ?, ?, ?)");
            int column = 0;
            ps.setLong(++column, customerId);
            ps.setString(++column, "");
            ps.setString(++column, signature);
            ps.setTimestamp(++column, new Timestamp(invokedAtMillis));
            return ps;
        }
    }
}
