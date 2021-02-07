/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.intake.file_import.impl

import io.codekvast.common.customer.CustomerService
import io.codekvast.common.logging.LoggerDelegate
import io.codekvast.database.DatabaseLimits
import io.codekvast.javaagent.model.v2.CommonPublicationData2
import io.codekvast.javaagent.model.v2.SignatureStatus2
import io.codekvast.javaagent.model.v3.CodeBaseEntry3
import io.codekvast.javaagent.model.v3.MethodSignature3
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import java.sql.*
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/** @author olle.hallin@crisp.se
 */
@Component
class ImportDAOImpl(
    private val jdbcTemplate: JdbcTemplate,
    private val customerService: CustomerService
) : ImportDAO {

    private val logger by LoggerDelegate()

    override fun importApplication(commonData: CommonPublicationData2): Long {
        val customerId: Long = commonData.customerId
        val name: String = commonData.appName
        val createdAt = Timestamp(commonData.jvmStartedAtMillis)
        val updated = jdbcTemplate.update(
            """UPDATE applications SET createdAt = LEAST(createdAt, ?) 
                   WHERE customerId = ? AND name = ? ORDER BY id """,
            createdAt, customerId, name
        )

        if (updated != 0) {
            logger.trace("Updated application {}", name)
        } else {
            jdbcTemplate.update(
                "INSERT INTO applications(customerId, name, createdAt) VALUES (?, ?, ?)",
                customerId,
                name,
                createdAt
            )
            logger.info(
                "Imported new application: customerId={}, name='{}', createdAt={}",
                customerId,
                name,
                createdAt
            )
        }
        val result = jdbcTemplate.queryForObject(
            "SELECT id FROM applications WHERE customerId = ? AND name = ?",
            Long::class.java,
            customerId,
            name
        )
        logger.debug("Application {}:'{}' has id {}", customerId, name, result)
        return result
    }

    override fun importEnvironment(commonData: CommonPublicationData2): Long {
        val customerId: Long = commonData.customerId
        var name: String = commonData.environment
        if (name.trim { it <= ' ' }.isEmpty()) {
            name = DEFAULT_ENVIRONMENT_NAME
        }
        val createdAt = Timestamp(commonData.jvmStartedAtMillis)
        val updated = jdbcTemplate.update(
            """UPDATE environments SET createdAt = LEAST(createdAt, ?) 
                   WHERE customerId = ? AND name = ? ORDER BY id """,
            createdAt, customerId, name
        )
        if (updated != 0) {
            logger.trace("Updated environment {}:'{}'", customerId, name)
        } else {
            jdbcTemplate.update(
                """INSERT INTO environments(customerId, name, createdAt, enabled) 
                       VALUES (?, ?, ?, TRUE)""",
                customerId, name, createdAt
            )
            logger.info(
                "Imported new environment: customerId={}, name='{}', createdAt={}",
                customerId, name, createdAt
            )
        }
        val result = jdbcTemplate.queryForObject(
            "SELECT id FROM environments WHERE customerId = ? AND name = ?",
            Long::class.java,
            customerId,
            name
        )
        logger.debug("Environment {}:'{}' has id {}", customerId, name, result)
        return result
    }

    override fun importJvm(
        commonData: CommonPublicationData2,
        applicationId: Long,
        environmentId: Long
    ): Long {
        val customerId: Long = commonData.customerId
        val publishedAt = Timestamp(commonData.publishedAtMillis)
        val updated = jdbcTemplate.update(
            "UPDATE jvms SET codeBaseFingerprint = ?, publishedAt = ?, garbage = FALSE WHERE uuid = ? "
                    + "ORDER BY id ",
            commonData.codeBaseFingerprint, publishedAt, commonData.jvmUuid
        )
        if (updated != 0) {
            logger.trace("Updated JVM {}", commonData.jvmUuid)
        } else {
            jdbcTemplate.update(
                """INSERT INTO jvms(customerId, applicationId, applicationVersion, environmentId, 
                       uuid, codeBaseFingerprint, startedAt, publishedAt, methodVisibility, packages, 
                       excludePackages, computerId, hostname, agentVersion, tags, garbage) 
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)""",
                customerId, applicationId, commonData.appVersion, environmentId, commonData.jvmUuid,
                commonData.codeBaseFingerprint, Timestamp(commonData.jvmStartedAtMillis),
                publishedAt, commonData.methodVisibility, commonData.packages.toString(),
                commonData.excludePackages.toString(), commonData.computerId, commonData.hostname,
                commonData.agentVersion, commonData.tags
            )
            logger.info(
                "Imported new JVM: customerId={}, applicationId={}, environmentId={}, jvmUUid='{}', startedAt={}",
                customerId,
                applicationId,
                environmentId,
                commonData.jvmUuid,
                Instant.ofEpochMilli(commonData.jvmStartedAtMillis)
            )
        }
        val result = jdbcTemplate.queryForObject(
            "SELECT id FROM jvms WHERE uuid = ?", Long::class.java, commonData.jvmUuid
        )
        logger.debug("JVM with uuid '{}' has id {}", commonData.jvmUuid, result)
        return result
    }

    override fun importCodeBaseFingerprint(
        data: CommonPublicationData2,
        importContext: CommonImporter.ImportContext
    ): Boolean {
        val customerId: Long = importContext.customerId
        val applicationId: Long = importContext.appId
        val codeBaseFingerprint: String = data.codeBaseFingerprint
        val publishedAt = Timestamp(data.publishedAtMillis)
        val updated = jdbcTemplate.update(
            "UPDATE codebase_fingerprints SET publishedAt = ? "
                    + "WHERE customerId = ? "
                    + "AND applicationId = ? "
                    + "AND codeBaseFingerprint = ? "
                    + "ORDER BY id ",
            publishedAt,
            customerId,
            applicationId,
            codeBaseFingerprint
        )
        if (updated > 0) {
            logger.debug(
                "Already imported codebase: {}:{}:{}",
                customerId,
                importContext.appId,
                codeBaseFingerprint
            )
            return false
        }
        val inserted = jdbcTemplate.update(
            "INSERT INTO codebase_fingerprints(customerId, applicationId, codeBaseFingerprint, publishedAt) "
                    + "VALUES(?, ?, ?, ?)",
            customerId,
            applicationId,
            codeBaseFingerprint,
            publishedAt
        )
        if (inserted != 1) {
            logger.error(
                "Failed to imported codebase: {}:{}:{}",
                customerId,
                importContext.appId,
                codeBaseFingerprint
            )
            return false
        }
        logger.info(
            "Imported codebase {}:{}:{}",
            customerId,
            applicationId,
            codeBaseFingerprint
        )
        return true
    }

    override fun importMethods(
        data: CommonPublicationData2,
        importContext: CommonImporter.ImportContext,
        entries: Collection<CodeBaseEntry3>
    ) {
        val customerId: Long = importContext.customerId
        val appId: Long = importContext.appId
        val publishedAtMillis: Long = importContext.publishedAtMillis
        val environmentId: Long = importContext.environmentId
        val existingPackages = getExistingPackages(customerId)
        val existingTypes = getExistingTypes(customerId)
        val existingMethods = getExistingMethods(customerId)
        val incompleteMethods = getIncompleteMethods(customerId)
        val invocationsNotFoundInCodeBase = getInvocationsNotFoundInCodeBase(customerId)
        val existingMethodLocations = getExistingMethodLocations(customerId)
        importNewPackages(customerId, publishedAtMillis, entries, existingPackages)
        importNewTypes(customerId, publishedAtMillis, entries, existingTypes)
        importNewMethods(customerId, publishedAtMillis, entries, existingMethods)
        insertMethodLocations(customerId, entries, existingMethods, existingMethodLocations)
        updateIncompleteMethods(
            customerId,
            publishedAtMillis,
            entries,
            incompleteMethods,
            existingMethods,
            invocationsNotFoundInCodeBase
        )

        val now: Instant = Instant.now()

        ensureInitialInvocations(
            data,
            customerId,
            appId,
            environmentId,
            entries,
            existingMethods,
            now
        )
        removeStaleInvocations(customerId, appId, environmentId, now, existingMethods)
        customerService.assertDatabaseSize(customerId)
    }

    private fun importNewPackages(
        customerId: Long,
        publishedAtMillis: Long,
        entries: Collection<CodeBaseEntry3>,
        existingPackages: MutableSet<String>
    ) {
        val startedAt: Instant = Instant.now()
        val createdAt = Timestamp(publishedAtMillis)
        var count = 0
        for (entry in entries) {
            val methodSignature: MethodSignature3? = entry.methodSignature
            if (methodSignature == null) {
                logger.warn(
                    "Cannot import package name from {}, no methodSignature",
                    entry
                )
            } else {
                val packageName: String = methodSignature.packageName
                if (!existingPackages.contains(packageName)) {
                    val updated = jdbcTemplate.update(
                        "INSERT INTO packages(customerId, name, createdAt) VALUES (?, ?, ?)",
                        customerId,
                        packageName,
                        createdAt
                    )
                    if (updated == 1) {
                        existingPackages.add(packageName)
                    } else {
                        logger.warn(
                            "Failed to insert {}:{} into packages",
                            customerId,
                            packageName
                        )
                    }
                    count += 1
                }
            }
        }
        logger.debug(
            "Imported {} packages in {} ms", count, Duration.between(startedAt, Instant.now())
        )
    }

    private fun importNewTypes(
        customerId: Long,
        publishedAtMillis: Long,
        entries: Collection<CodeBaseEntry3>,
        existingTypes: MutableSet<String>
    ) {
        val startedAt: Instant = Instant.now()
        val createdAt = Timestamp(publishedAtMillis)
        var count = 0
        for (entry in entries) {
            val methodSignature: MethodSignature3? = entry.methodSignature
            if (methodSignature == null) {
                logger.warn(
                    "Cannot import declaring type from {}, no methodSignature",
                    entry
                )
            } else {
                val declaringType: String = methodSignature.declaringType
                if (!existingTypes.contains(declaringType)) {
                    val updated = jdbcTemplate.update(
                        "INSERT INTO types(customerId, name, createdAt) VALUES (?, ?, ?)",
                        customerId,
                        declaringType,
                        createdAt
                    )
                    if (updated == 1) {
                        existingTypes.add(declaringType)
                    } else {
                        logger.warn(
                            "Failed to insert {}:{} into types",
                            customerId,
                            declaringType
                        )
                    }
                    count += 1
                }
            }
        }
        logger.debug(
            "Imported {} packages in {} ms", count, Duration.between(startedAt, Instant.now())
        )
    }

    override fun importInvocations(
        importContext: CommonImporter.ImportContext,
        recordingIntervalStartedAtMillis: Long,
        invocations: Set<String>
    ) {
        val customerId: Long = importContext.customerId
        val appId: Long = importContext.appId
        val environmentId: Long = importContext.environmentId
        val existingMethods = getExistingMethods(customerId)

        doImportInvocations(
            customerId,
            appId,
            environmentId,
            recordingIntervalStartedAtMillis,
            invocations,
            existingMethods
        )
        customerService.assertDatabaseSize(customerId)
    }

    override fun upsertApplicationDescriptor(
        data: CommonPublicationData2, appId: Long, environmentId: Long
    ) {
        jdbcTemplate.update(
            UpsertApplicationDescriptorStatement(
                data.customerId,
                appId,
                environmentId,
                data.jvmStartedAtMillis,
                data.publishedAtMillis
            )
        )
    }

    private fun doImportInvocations(
        customerId: Long,
        appId: Long,
        environmentId: Long,
        invokedAtMillis: Long,
        invokedSignatures: Set<String>,
        existingMethods: MutableMap<String, Long?>
    ) {
        for (sig in invokedSignatures) {
            val signature: String = DatabaseLimits.normalizeSignature(sig)
            var methodId = existingMethods[signature]
            if (methodId == null) {
                logger.trace("Inserting incomplete method {}:{}", methodId, signature)
                methodId = doInsertRow(
                    InsertIncompleteMethodStatement(customerId, signature, invokedAtMillis)
                )
                existingMethods[signature] = methodId
            }
            logger.trace("Upserting invocation {}", signature)
            jdbcTemplate.update(
                UpsertInvocationStatement(
                    customerId,
                    appId,
                    environmentId,
                    methodId,
                    SignatureStatus2.INVOKED,
                    invokedAtMillis,
                    null
                )
            )
        }
    }

    private fun getExistingPackages(customerId: Long): MutableSet<String> {
        return HashSet(
            jdbcTemplate.queryForList(
                "SELECT name FROM packages WHERE customerId = ?", String::class.java, customerId
            )
        )
    }

    private fun getExistingTypes(customerId: Long): MutableSet<String> {
        return HashSet(
            jdbcTemplate.queryForList(
                "SELECT name FROM types WHERE customerId = ?", String::class.java, customerId
            )
        )
    }

    private fun getExistingMethods(customerId: Long): MutableMap<String, Long?> {
        val result: MutableMap<String, Long?> = HashMap()
        jdbcTemplate.query(
            "SELECT id, signature FROM methods WHERE customerId = ? ",
            { rs: ResultSet -> result[rs.getString(2)] = rs.getLong(1) },
            customerId
        )
        return result
    }

    private fun getIncompleteMethods(customerId: Long): Set<String> {
        return HashSet(
            jdbcTemplate.queryForList(
                "SELECT signature FROM methods WHERE customerId = ? AND methodName IS NULL ",
                String::class.java,
                customerId
            )
        )
    }

    private fun getExistingMethodLocations(customerId: Long): MutableSet<Long> {
        return HashSet(
            jdbcTemplate.queryForList(
                "SELECT methodId FROM method_locations WHERE customerId = ? ",
                Long::class.java,
                customerId
            )
        )
    }

    private fun getInvocationsNotFoundInCodeBase(customerId: Long): Set<Long> {
        return HashSet(
            jdbcTemplate.queryForList(
                "SELECT methodId FROM invocations WHERE customerId = ? AND status = ?",
                Long::class.java,
                customerId,
                SignatureStatus2.NOT_FOUND_IN_CODE_BASE.name
            )
        )
    }

    private fun importNewMethods(
        customerId: Long,
        publishedAtMillis: Long,
        entries: Collection<CodeBaseEntry3>,
        existingMethods: MutableMap<String, Long?>
    ) {
        val startedAt: Instant = Instant.now()
        var count = 0
        for (entry in entries) {
            val signature = truncateTooLongSignature(customerId, entry.signature)
            if (!existingMethods.containsKey(signature)) {
                existingMethods[signature] = doInsertRow(
                    InsertCompleteMethodStatement(
                        customerId,
                        publishedAtMillis,
                        entry.methodSignature,
                        entry.visibility,
                        signature
                    )
                )
                count += 1
            }
        }
        logger.debug(
            "Imported {} methods in {} ms",
            count,
            Duration.between(startedAt, Instant.now())
        )
    }

    private fun truncateTooLongSignature(customerId: Long, signature: String): String {
        if (signature.length > DatabaseLimits.MAX_METHOD_SIGNATURE_LENGTH) {
            val inserted = jdbcTemplate.update(
                "INSERT IGNORE INTO truncated_signatures(customerId, signature, length, truncatedLength) VALUES(?, ?, ?, ?)",
                customerId,
                signature,
                signature.length,
                DatabaseLimits.MAX_METHOD_SIGNATURE_LENGTH
            )
            if (inserted > 0) {
                logger.warn(
                    "Too long signature {}:'{}' ({} characters), longer than {} characters, will be truncated.",
                    customerId,
                    signature,
                    signature.length,
                    DatabaseLimits.MAX_METHOD_SIGNATURE_LENGTH
                )
            }
            return DatabaseLimits.normalizeSignature(signature)
        }
        return signature
    }

    private fun insertMethodLocations(
        customerId: Long,
        entries: Collection<CodeBaseEntry3>,
        existingMethods: MutableMap<String, Long?>,
        existingMethodLocations: MutableSet<Long>
    ) {
        val startedAt: Instant = Instant.now()
        var count = 0
        for (entry in entries) {
            val location: String? = entry.methodSignature.location
            val signature: String = DatabaseLimits.normalizeSignature(entry.signature)
            val methodId = existingMethods[signature]!!
            if (location != null && !existingMethodLocations.contains(methodId)) {
                logger.debug("Inserting {} ({})", signature, location)
                count += jdbcTemplate.update(
                    InsertMethodLocationStatement(
                        customerId,
                        methodId,
                        location
                    )
                )
                existingMethodLocations.add(methodId)
            }
        }
        logger.debug(
            "Inserted {} method locations in {} ms",
            count,
            Duration.between(startedAt, Instant.now())
        )
    }

    private fun updateIncompleteMethods(
        customerId: Long,
        publishedAtMillis: Long,
        entries: Collection<CodeBaseEntry3>,
        incompleteMethods: Set<String>,
        existingMethods: Map<String, Long?>,
        incompleteInvocations: Set<Long>
    ) {
        val startedAt: Instant = Instant.now()
        var count = 0
        for (entry in entries) {
            val signature: String = DatabaseLimits.normalizeSignature(entry.signature)
            val methodId = existingMethods[signature]
            if (incompleteMethods.contains(signature) || incompleteInvocations.contains(methodId)) {
                logger.debug("Updating {}", signature)
                jdbcTemplate.update(
                    UpdateIncompleteMethodStatement(customerId, publishedAtMillis, entry)
                )
                count += 1
            }
        }
        logger.debug(
            "Updated {} incomplete methods in {} ms",
            count,
            Duration.between(startedAt, Instant.now())
        )
    }

    private fun ensureInitialInvocations(
        data: CommonPublicationData2,
        customerId: Long,
        appId: Long,
        environmentId: Long,
        entries: Collection<CodeBaseEntry3>,
        existingMethods: MutableMap<String, Long?>,
        now: Instant
    ) {
        val startedAt: Instant = Instant.now()
        var importCount = 0
        for (entry in entries) {
            val signature: String = DatabaseLimits.normalizeSignature(entry.signature)
            val methodId = existingMethods[signature]!!
            val initialStatus: SignatureStatus2 = calculateInitialStatus(data, entry)
            val updated: Int = jdbcTemplate.update(
                UpsertInvocationStatement(
                    customerId, appId, environmentId, methodId, initialStatus, 0L, now
                )
            )
            importCount += if (updated == 1) 1 else 0
        }
        logger.debug(
            "Imported {} initial invocations in {} ms",
            importCount,
            Duration.between(startedAt, Instant.now())
        )
    }

    private fun calculateInitialStatus(
        data: CommonPublicationData2, entry: CodeBaseEntry3
    ): SignatureStatus2 {
        for (pkg in data.excludePackages) {
            if (entry.methodSignature.packageName.startsWith(pkg)) {
                return SignatureStatus2.EXCLUDED_BY_PACKAGE_NAME
            }
        }
        return Optional.ofNullable(getExcludeByVisibility(data.methodVisibility, entry))
            .orElse(getExcludeByTriviality(entry))
    }

    private fun getExcludeByTriviality(entry: CodeBaseEntry3): SignatureStatus2 {
        val name: String = entry.methodSignature.methodName
        val parameterTypes: String =
            entry.methodSignature.parameterTypes.trim { it <= ' ' }
        val noParameters = parameterTypes.isEmpty()
        val singleParameter = parameterTypes.isNotEmpty() && !parameterTypes.contains(",")
        if (name == "hashCode" && noParameters) {
            return SignatureStatus2.EXCLUDED_SINCE_TRIVIAL
        }
        if (name == "equals" && singleParameter) {
            return SignatureStatus2.EXCLUDED_SINCE_TRIVIAL
        }
        if (name == "canEqual" && singleParameter) {
            return SignatureStatus2.EXCLUDED_SINCE_TRIVIAL
        }
        if (name == "compareTo" && singleParameter) {
            return SignatureStatus2.EXCLUDED_SINCE_TRIVIAL
        }
        return if (name == "toString" && noParameters) {
            SignatureStatus2.EXCLUDED_SINCE_TRIVIAL
        } else SignatureStatus2.NOT_INVOKED
    }

    fun getExcludeByVisibility(
        methodVisibility: String?,
        entry: CodeBaseEntry3
    ): SignatureStatus2? {
        val v: String = entry.visibility
        when (methodVisibility) {
            VISIBILITY_PRIVATE -> return null
            VISIBILITY_PACKAGE_PRIVATE -> return if (v == VISIBILITY_PUBLIC || v == VISIBILITY_PROTECTED || v == VISIBILITY_PACKAGE_PRIVATE) null else SignatureStatus2.EXCLUDED_BY_VISIBILITY
            VISIBILITY_PROTECTED -> return if (v == VISIBILITY_PUBLIC || v == VISIBILITY_PROTECTED) null else SignatureStatus2.EXCLUDED_BY_VISIBILITY
            VISIBILITY_PUBLIC -> return if (v == VISIBILITY_PUBLIC) null else SignatureStatus2.EXCLUDED_BY_VISIBILITY
        }
        return null
    }

    private fun removeStaleInvocations(
        customerId: Long,
        appId: Long,
        environmentId: Long,
        now: Instant,
        existingMethods: Map<String, Long?>
    ) {

        // TODO: Convert to a simple DELETE once the erroneous stale deletions have been fixed
        val methodsById = existingMethods.entries.stream()
            .collect(Collectors.toMap({ it.value }, { it.key }) { _, b -> b })
        val deleted = AtomicInteger(0)
        jdbcTemplate.query(
            "DELETE FROM invocations WHERE customerId = ? AND applicationId = ? AND environmentId = ? "
                    + "AND lastSeenAtMillis < ? AND status <> ? "
                    + "RETURNING methodId, status, invokedAtMillis, createdAt, lastSeenAtMillis, timestamp ",
            { rs: ResultSet ->
                val methodId: Long = rs.getLong("methodId")
                logger.info(
                    "Removed stale invocation {}:{}:{}:{} ('{}'), {}, createdAt={}, lastSeenAt={}, timestamp={}, now={}",
                    customerId,
                    appId,
                    environmentId,
                    methodId,
                    methodsById[methodId],
                    formatInvokedAt(rs.getString("status"), rs.getLong("invokedAtMillis")),
                    rs.getTimestamp("createdAt").toInstant(),
                    Instant.ofEpochMilli(rs.getLong("lastSeenAtMillis")),
                    rs.getTimestamp("timestamp").toInstant(),
                    now
                )
                deleted.incrementAndGet()
            },
            customerId,
            appId,
            environmentId,
            now.toEpochMilli(),
            SignatureStatus2.INVOKED.name
        )
        if (deleted.get() > 0) {
            logger.info(
                "Removed {} stale invocations for {}:{}:{}",
                deleted,
                customerId,
                appId,
                environmentId
            )
        } else {
            logger.debug(
                "Removed no stale invocations for {}:{}:{}",
                customerId,
                appId,
                environmentId
            )
        }
    }

    private fun formatInvokedAt(status: String, invokedAtMillis: Long): String {
        return if (invokedAtMillis == 0L) status else "$status at ${
            Instant.ofEpochMilli(
                invokedAtMillis
            )
        }"
    }

    private fun doInsertRow(psc: PreparedStatementCreator): Long {
        val keyHolder: KeyHolder = GeneratedKeyHolder()
        jdbcTemplate.update(psc, keyHolder)
        return keyHolder.key!!.toLong()
    }

    private class InsertCompleteMethodStatement(
        private val customerId: Long,
        private val publishedAtMillis: Long,
        private val method: MethodSignature3,
        private val visibility: String,
        private val signature: String
    ) : PreparedStatementCreator {

        override fun createPreparedStatement(con: Connection): PreparedStatement {
            val ps: PreparedStatement = con.prepareStatement(
                """INSERT INTO methods(customerId, visibility, signature, createdAt,
                       declaringType, exceptionTypes, methodName, bridge, synthetic, modifiers,
                       packageName, parameterTypes, returnType) 
                       VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                Statement.RETURN_GENERATED_KEYS
            )
            var column = 0
            ps.setLong(++column, customerId)
            ps.setString(++column, visibility)
            ps.setString(++column, signature)
            ps.setTimestamp(++column, Timestamp(publishedAtMillis))
            ps.setString(++column, method.declaringType)
            ps.setString(++column, method.exceptionTypes)
            ps.setString(++column, method.methodName)
            ps.setObject(++column, method.bridge, Types.BOOLEAN)
            ps.setObject(++column, method.synthetic, Types.BOOLEAN)
            ps.setString(++column, method.modifiers)
            ps.setString(++column, method.packageName)
            ps.setString(++column, method.parameterTypes)
            ps.setString(++column, method.returnType)
            return ps
        }
    }

    private class UpdateIncompleteMethodStatement(
        private val customerId: Long,
        private val publishedAtMillis: Long,
        private val entry: CodeBaseEntry3
    ) : PreparedStatementCreator {

        override fun createPreparedStatement(con: Connection): PreparedStatement {
            val ps: PreparedStatement = con.prepareStatement(
                """UPDATE methods
                       SET visibility = ?, createdAt = LEAST(createdAt, ?), declaringType = ?,
                           exceptionTypes = ?, methodName = ?, bridge = ?, synthetic = ?, 
                           modifiers = ?, packageName = ?, parameterTypes = ?, returnType = ?
                       WHERE customerId = ? AND signature = ? ORDER BY id """
            )
            var column = 0
            val method: MethodSignature3 = entry.methodSignature
            ps.setString(++column, entry.visibility)
            ps.setTimestamp(++column, Timestamp(publishedAtMillis))
            ps.setString(++column, method.declaringType)
            ps.setString(++column, method.exceptionTypes)
            ps.setString(++column, method.methodName)
            ps.setObject(++column, method.bridge, Types.BOOLEAN)
            ps.setObject(++column, method.synthetic, Types.BOOLEAN)
            ps.setString(++column, method.modifiers)
            ps.setString(++column, method.packageName)
            ps.setString(++column, method.parameterTypes)
            ps.setString(++column, method.returnType)
            ps.setLong(++column, customerId)
            ps.setString(++column, DatabaseLimits.normalizeSignature(entry.signature))
            return ps
        }
    }

    private class UpsertInvocationStatement(
        private val customerId: Long,
        private val appId: Long,
        private val environmentId: Long,
        private val methodId: Long,
        private val status: SignatureStatus2,
        private val invokedAtMillis: Long,
        private val lastSeenAt: Instant?
    ) : PreparedStatementCreator {

        override fun createPreparedStatement(con: Connection): PreparedStatement {
            val sqlCodebase =
                """INSERT INTO invocations(customerId, applicationId, environmentId, methodId, status, invokedAtMillis, lastSeenAtMillis) 
                   VALUES(?, ?, ?, ?, ?, ?, ?) 
                   ON DUPLICATE KEY UPDATE lastSeenAtMillis = ? """
            val sqlInvocation =
                """INSERT INTO invocations(customerId, applicationId, environmentId, methodId, status, invokedAtMillis)
                   VALUES(?, ?, ?, ?, ?, ?)
                   ON DUPLICATE KEY UPDATE invokedAtMillis = GREATEST(invokedAtMillis, VALUE(invokedAtMillis)), status = VALUE(status)"""
            val ps: PreparedStatement =
                con.prepareStatement(if (invokedAtMillis == 0L) sqlCodebase else sqlInvocation)

            var column = 0
            ps.setLong(++column, customerId)
            ps.setLong(++column, appId)
            ps.setLong(++column, environmentId)
            ps.setLong(++column, methodId)
            ps.setString(++column, status.name)
            ps.setLong(++column, invokedAtMillis)
            if (invokedAtMillis == 0L) { // codebase
                ps.setLong(++column, lastSeenAt!!.toEpochMilli()) // insert
                ps.setLong(++column, lastSeenAt.toEpochMilli()) // update
            }
            return ps
        }
    }

    private class InsertMethodLocationStatement(
        private val customerId: Long,
        private val methodId: Long,
        private val location: String
    ) : PreparedStatementCreator {

        override fun createPreparedStatement(con: Connection): PreparedStatement {
            val ps: PreparedStatement = con.prepareStatement(
                "INSERT INTO method_locations(customerId, methodId, location) VALUES(?, ?, ?) "
            )
            var column = 0
            ps.setLong(++column, customerId)
            ps.setLong(++column, methodId)
            ps.setString(++column, location)
            return ps
        }
    }

    private class InsertIncompleteMethodStatement(
        private val customerId: Long,
        private val signature: String,
        private val invokedAtMillis: Long
    ) : PreparedStatementCreator {

        override fun createPreparedStatement(con: Connection): PreparedStatement {
            val ps: PreparedStatement = con.prepareStatement(
                "INSERT INTO methods(customerId, visibility, signature, createdAt) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            var column = 0
            ps.setLong(++column, customerId)
            ps.setString(++column, "")
            ps.setString(++column, signature)
            ps.setTimestamp(++column, Timestamp(invokedAtMillis))
            return ps
        }
    }

    private class UpsertApplicationDescriptorStatement(
        private val customerId: Long,
        private val appId: Long,
        private val environmentId: Long,
        private val jvmStartedAtMillis: Long,
        private val publishedAtMillis: Long
    ) : PreparedStatementCreator {


        override fun createPreparedStatement(con: Connection): PreparedStatement {
            val sql =
                ("INSERT INTO application_descriptors(customerId, applicationId, environmentId, collectedSince, collectedTo) "
                        + "VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE collectedTo = GREATEST(?, collectedTo)")
            val collectedSince = Timestamp(jvmStartedAtMillis)
            val collectedTo = Timestamp(publishedAtMillis)
            val ps: PreparedStatement = con.prepareStatement(sql)
            var column = 0
            ps.setLong(++column, customerId)
            ps.setLong(++column, appId)
            ps.setLong(++column, environmentId)
            ps.setTimestamp(++column, collectedSince)
            ps.setTimestamp(++column, collectedTo) // insert
            ps.setTimestamp(++column, collectedTo) // update
            return ps
        }
    }

    companion object {
        private const val VISIBILITY_PRIVATE = "private"
        private const val VISIBILITY_PACKAGE_PRIVATE = "package-private"
        private const val VISIBILITY_PROTECTED = "protected"
        private const val VISIBILITY_PUBLIC = "public"
        private const val DEFAULT_ENVIRONMENT_NAME = "<default>"
    }
}