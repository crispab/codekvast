/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
package io.codekvast.dashboard.file_import.impl;

import io.codekvast.common.customer.CustomerService;
import io.codekvast.javaagent.model.v2.CodeBaseEntry2;
import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import io.codekvast.javaagent.model.v2.MethodSignature2;
import io.codekvast.javaagent.model.v2.SignatureStatus2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.Instant;
import java.util.*;

import static java.sql.Types.BOOLEAN;
import static java.util.Optional.ofNullable;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportDAOImpl implements ImportDAO {

    private static final String VISIBILITY_PRIVATE = "private";
    private static final String VISIBILITY_PACKAGE_PRIVATE = "package-private";
    private static final String PROTECTED = "protected";
    private static final String VISIBILITY_PUBLIC = "public";
    private static final String DEFAULT_ENVIRONMENT_NAME = "<default>";
    private final JdbcTemplate jdbcTemplate;
    private final CustomerService customerService;

    @Override
    public long importApplication(CommonPublicationData2 data) {
        long customerId = data.getCustomerId();
        String name = data.getAppName();
        String version = data.getAppVersion();
        Timestamp createdAt = new Timestamp(data.getJvmStartedAtMillis());

        int updated = jdbcTemplate.update("UPDATE applications SET createdAt = LEAST(createdAt, ?) " +
                                              "WHERE customerId = ? AND name = ? AND version = ?", createdAt, customerId, name, version);
        if (updated != 0) {
            logger.trace("Updated application {} {}", name, version);
        } else {
            jdbcTemplate.update("INSERT INTO applications(customerId, name, version, createdAt) VALUES (?, ?, ?, ?)",
                                customerId, name, version, createdAt);
            logger.trace("Inserted application {} {} {} {}", customerId, name, version, createdAt);
        }

        Long result = jdbcTemplate
            .queryForObject("SELECT id FROM applications WHERE customerId = ? AND name = ? AND version = ?", Long.class, customerId, name,
                            version);
        logger.debug("application {} {} {} has id {}", customerId, name, version, result);
        return result;
    }

    @Override
    public long importEnvironment(CommonPublicationData2 data) {
        long customerId = data.getCustomerId();
        String name = data.getEnvironment();
        if (name.trim().isEmpty()) {
            name = DEFAULT_ENVIRONMENT_NAME;
        }
        Timestamp createdAt = new Timestamp(data.getJvmStartedAtMillis());

        int updated = jdbcTemplate.update("UPDATE environments SET createdAt = LEAST(createdAt, ?) " +
                                              "WHERE customerId = ? AND name = ?", createdAt, customerId, name);
        if (updated != 0) {
            logger.trace("Updated environment {}", name);
        } else {
            jdbcTemplate.update("INSERT INTO environments(customerId, name, createdAt) VALUES (?, ?, ?)", customerId, name, createdAt);
            logger.trace("Inserted environment {} {} {}", customerId, name, createdAt);
        }

        Long result = jdbcTemplate
            .queryForObject("SELECT id FROM environments WHERE customerId = ? AND name = ?", Long.class, customerId, name);
        logger.debug("environment {} {} has id {}", customerId, name, result);
        return result;
    }

    @Override
    public long importJvm(CommonPublicationData2 data, long applicationId, long environmentId) {

        long customerId = data.getCustomerId();
        Timestamp publishedAt = new Timestamp(data.getPublishedAtMillis());

        int updated = jdbcTemplate.update("UPDATE jvms SET publishedAt = ? WHERE uuid = ?",
                                          publishedAt, data.getJvmUuid());
        if (updated != 0) {
            logger.trace("Updated JVM {}", data.getJvmUuid());
        } else {
            jdbcTemplate.update(
                "INSERT INTO jvms(customerId, applicationId, environmentId, uuid, startedAt, publishedAt, methodVisibility, packages," +
                    " excludePackages, computerId, hostname, agentVersion, tags) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                customerId, applicationId, environmentId, data.getJvmUuid(), new Timestamp(data.getJvmStartedAtMillis()), publishedAt,
                data.getMethodVisibility(), data.getPackages().toString(), data.getExcludePackages().toString(), data.getComputerId(),
                data.getHostname(), data.getAgentVersion(), data.getTags());
            logger.trace("Inserted jvm {} {} started at {}", customerId, data.getJvmUuid(),
                         Instant.ofEpochMilli(data.getJvmStartedAtMillis()));
        }

        Long result = jdbcTemplate.queryForObject("SELECT id FROM jvms WHERE uuid = ?", Long.class, data.getJvmUuid());

        logger.debug("JVM with uuid {} has id {}", data.getJvmUuid(), result);
        return result;
    }

    @Override
    public void importMethods(CommonPublicationData2 data, long customerId, long appId, long environmentId, long jvmId,
                              long publishedAtMillis, Collection<CodeBaseEntry2> entries) {
        Map<String, Long> existingMethods = getExistingMethods(customerId);
        Set<String> incompleteMethods = getIncompleteMethods(customerId);
        Set<Long> invocationsNotFoundInCodeBase = getInvocationsNotFoundInCodeBase(customerId);
        Set<Long> existingInvocations = getExistingInvocations(customerId, appId, jvmId);

        importNewMethods(customerId, publishedAtMillis, entries, existingMethods);
        updateIncompleteMethods(customerId, publishedAtMillis, entries, incompleteMethods, existingMethods, invocationsNotFoundInCodeBase);
        ensureInitialInvocations(data, customerId, appId, environmentId, jvmId, entries, existingMethods, existingInvocations);

        customerService.assertDatabaseSize(customerId);
    }

    @Override
    public void importInvocations(long customerId, long appId, long environmentId, long jvmId, long invokedAtMillis, Set<String> invocations) {
        Map<String, Long> existingMethods = getExistingMethods(customerId);
        Set<Long> existingInvocations = getExistingInvocations(customerId, appId, jvmId);

        doImportInvocations(customerId, appId, environmentId, jvmId, invokedAtMillis, invocations, existingMethods, existingInvocations);

        customerService.assertDatabaseSize(customerId);
    }

    private void doImportInvocations(long customerId, long appId, long environmentId, long jvmId, long invokedAtMillis, Set<String> invokedSignatures,
                                     Map<String, Long> existingMethods, Set<Long> existingInvocations) {
        for (String signature : invokedSignatures) {
            Long methodId = existingMethods.get(signature);
            if (methodId == null) {
                logger.trace("Inserting incomplete method {}:{}", methodId, signature);
                existingMethods.put(signature, doInsertRow(new InsertIncompleteMethodStatement(customerId, signature, invokedAtMillis)));
                methodId = existingMethods.get(signature);
            }
            if (existingInvocations.contains(methodId)) {
                logger.trace("Updating invocation {}", signature);
                jdbcTemplate.update(new UpdateInvocationStatement(customerId, appId, jvmId, methodId, invokedAtMillis));
            } else {
                logger.trace("Inserting invocation {}", signature);
                jdbcTemplate
                    .update(new InsertInvocationStatement(customerId, appId, environmentId, jvmId, methodId, SignatureStatus2.NOT_FOUND_IN_CODE_BASE,
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
                              customerId, SignatureStatus2.NOT_FOUND_IN_CODE_BASE.name())
        );
    }


    private Set<Long> getExistingInvocations(long customerId, long appId, long jvmId) {
        return new HashSet<>(
            jdbcTemplate
                .queryForList("SELECT methodId FROM invocations WHERE customerId = ? AND applicationId = ? AND jvmId = ?", Long.class,
                              customerId, appId, jvmId));
    }

    private void importNewMethods(long customerId, long publishedAtMillis, Collection<CodeBaseEntry2> entries,
                                  Map<String, Long> existingMethods) {
        long startedAtMillis = System.currentTimeMillis();
        int count = 0;
        for (CodeBaseEntry2 entry : entries) {
            String signature = entry.getSignature();
            if (!existingMethods.containsKey(signature)) {
                existingMethods
                    .put(signature, doInsertRow(new InsertCompleteMethodStatement(customerId, publishedAtMillis, entry.getMethodSignature(),
                                                                                  entry.getVisibility(), entry.getSignature())));
                count += 1;
            }
        }
        logger.debug("Imported {} methods in {} ms", count, System.currentTimeMillis() - startedAtMillis);
    }

    private void updateIncompleteMethods(long customerId, long publishedAtMillis, Collection<CodeBaseEntry2> entries,
                                         Set<String> incompleteMethods,
                                         Map<String, Long> existingMethods, Set<Long> incompleteInvocations) {
        long startedAtMillis = System.currentTimeMillis();
        int count = 0;
        for (CodeBaseEntry2 entry : entries) {
            long methodId = existingMethods.get(entry.getSignature());
            if (incompleteMethods.contains(entry.getSignature()) || incompleteInvocations.contains(methodId)) {
                logger.debug("Updating {}", entry.getSignature());
                jdbcTemplate.update(new UpdateIncompleteMethodStatement(customerId, publishedAtMillis, entry));
                count += 1;
            }
        }
        logger.debug("Updated {} incomplete methods in {} ms", count, System.currentTimeMillis() - startedAtMillis);
    }

    private void ensureInitialInvocations(CommonPublicationData2 data, long customerId, long appId, long environmentId,
                                          long jvmId, Collection<CodeBaseEntry2> entries,
                                          Map<String, Long> existingMethods, Set<Long> existingInvocations) {
        long startedAtMillis = System.currentTimeMillis();
        int importCount = 0;

        for (CodeBaseEntry2 entry : entries) {
            long methodId = existingMethods.get(entry.getSignature());
            if (!existingInvocations.contains(methodId)) {
                SignatureStatus2 initialStatus = calculateInitialStatus(data, entry);
                jdbcTemplate.update(new InsertInvocationStatement(customerId, appId, environmentId, jvmId, methodId, initialStatus,
                                                                  0L, 0L));
                existingInvocations.add(methodId);
                importCount += 1;
            }
        }
        logger.debug("Imported {} invocations in {} ms", importCount, System.currentTimeMillis() - startedAtMillis);
    }

    private SignatureStatus2 calculateInitialStatus(CommonPublicationData2 data, CodeBaseEntry2 entry) {
        for (String pkg : data.getExcludePackages()) {
            if (entry.getMethodSignature().getPackageName().startsWith(pkg)) {
                return SignatureStatus2.EXCLUDED_BY_PACKAGE_NAME;
            }
        }

        return ofNullable(getExcludeByVisibility(data.getMethodVisibility(), entry)).orElse(getExcludeByTriviality(entry));
    }

    private SignatureStatus2 getExcludeByTriviality(CodeBaseEntry2 entry) {
        String name = entry.getMethodSignature().getMethodName();
        String parameterTypes = entry.getMethodSignature().getParameterTypes().trim();

        boolean noParameters = parameterTypes.isEmpty();
        boolean singleParameter = !parameterTypes.isEmpty() && !parameterTypes.contains(",");

        if (name.equals("hashCode") && noParameters) {
            return SignatureStatus2.EXCLUDED_SINCE_TRIVIAL;
        }
        if (name.equals("equals") && singleParameter) {
            return SignatureStatus2.EXCLUDED_SINCE_TRIVIAL;
        }
        if (name.equals("compareTo") && singleParameter) {
            return SignatureStatus2.EXCLUDED_SINCE_TRIVIAL;
        }
        if (name.equals("toString") && noParameters) {
            return SignatureStatus2.EXCLUDED_SINCE_TRIVIAL;
        }

        return SignatureStatus2.NOT_INVOKED;
    }

    SignatureStatus2 getExcludeByVisibility(String methodVisibility, CodeBaseEntry2 entry) {
        String v = entry.getVisibility();
        switch (methodVisibility) {
        case VISIBILITY_PRIVATE:
            return null;
        case VISIBILITY_PACKAGE_PRIVATE:
            return v.equals(VISIBILITY_PUBLIC) || v.equals(PROTECTED) || v.equals(VISIBILITY_PACKAGE_PRIVATE) ? null :
                SignatureStatus2.EXCLUDED_BY_VISIBILITY;
        case PROTECTED:
            return v.equals(VISIBILITY_PUBLIC) || v.equals(PROTECTED) ? null : SignatureStatus2.EXCLUDED_BY_VISIBILITY;
        case VISIBILITY_PUBLIC:
            return v.equals(VISIBILITY_PUBLIC) ? null : SignatureStatus2.EXCLUDED_BY_VISIBILITY;
        }
        return null;
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
        private final MethodSignature2 method;
        private final String visibility;
        private final String signature;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {

            PreparedStatement ps =
                con.prepareStatement("INSERT INTO methods(customerId, visibility, signature, createdAt, declaringType, " +
                                         "exceptionTypes, methodName, bridge, synthetic, modifiers, packageName, parameterTypes, " +
                                         "returnType) " +
                                         "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                     Statement.RETURN_GENERATED_KEYS);
            int column = 0;
            ps.setLong(++column, customerId);
            ps.setString(++column, visibility);
            ps.setString(++column, signature);
            ps.setTimestamp(++column, new Timestamp(publishedAtMillis));
            ps.setString(++column, method.getDeclaringType());
            ps.setString(++column, method.getExceptionTypes());
            ps.setString(++column, method.getMethodName());
            ps.setObject(++column, method.getBridge(), BOOLEAN);
            ps.setObject(++column, method.getSynthetic(), BOOLEAN);
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
        private final CodeBaseEntry2 entry;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {

            PreparedStatement ps = con.prepareStatement(
                "UPDATE methods\n" +
                    "SET visibility   = ?, createdAt = LEAST(createdAt, ?), declaringType = ?, exceptionTypes = ?, methodName = ?,\n" +
                    "  bridge = ?, synthetic = ?, modifiers = ?, packageName    = ?, parameterTypes = ?, returnType = ?\n" +
                    "WHERE customerId = ? AND signature = ?");
            int column = 0;
            MethodSignature2 method = entry.getMethodSignature();
            ps.setString(++column, entry.getVisibility());
            ps.setTimestamp(++column, new Timestamp(publishedAtMillis));
            ps.setString(++column, method.getDeclaringType());
            ps.setString(++column, method.getExceptionTypes());
            ps.setString(++column, method.getMethodName());
            ps.setObject(++column, method.getBridge(), BOOLEAN);
            ps.setObject(++column, method.getSynthetic(), BOOLEAN);
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
        private final long environmentId;
        private final long jvmId;
        private final long methodId;
        private final SignatureStatus2 status;
        private final long invokedAtMillis;
        private final long invocationCount;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps =
                con.prepareStatement(
                    "INSERT INTO invocations(customerId, applicationId, environmentId, jvmId, methodId, status, invokedAtMillis, invocationCount) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
            int column = 0;
            ps.setLong(++column, customerId);
            ps.setLong(++column, appId);
            ps.setLong(++column, environmentId);
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
            ps.setString(++column, SignatureStatus2.INVOKED.name());
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
                con.prepareStatement("INSERT INTO methods(customerId, visibility, signature, createdAt) VALUES (?, ?, ?, ?)",
                                     Statement.RETURN_GENERATED_KEYS);
            int column = 0;
            ps.setLong(++column, customerId);
            ps.setString(++column, "");
            ps.setString(++column, signature);
            ps.setTimestamp(++column, new Timestamp(invokedAtMillis));
            return ps;
        }
    }
}
