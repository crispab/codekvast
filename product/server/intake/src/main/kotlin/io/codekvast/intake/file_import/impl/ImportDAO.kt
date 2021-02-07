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

import io.codekvast.javaagent.model.v2.CommonPublicationData2
import io.codekvast.javaagent.model.v3.CodeBaseEntry3

/**
 * Interface for importing publications to the database.
 *
 * @author olle.hallin@crisp.se
 */
interface ImportDAO {
    /**
     * Upserts a row in the applications table.
     *
     * @param commonData The application data
     * @return The primary key of the inserted or updated applications row.
     */
    fun importApplication(commonData: CommonPublicationData2): Long

    /**
     * Upserts a row in the environments table.
     *
     * @param commonData The JVM data
     * @return The primary key of the inserted or updated environments row.
     */
    fun importEnvironment(commonData: CommonPublicationData2): Long

    /**
     * Upserts a row in the jvms table.
     *
     * @param commonData The JVM data
     * @param applicationId The value returned from importApplication()
     * @param environmentId The value returned from importEnvironment()
     * @return The primary key of the inserted or updated jvms row.
     */
    fun importJvm(
        commonData: CommonPublicationData2,
        applicationId: Long,
        environmentId: Long
    ): Long

    /**
     * Import a codebase fingerprint.
     *
     * @param data The common publication data
     * @param importContext The customerId and appId to use.
     * @return true if-and-only-if this codebase has not been imported before.
     */
    fun importCodeBaseFingerprint(
        data: CommonPublicationData2,
        importContext: CommonImporter.ImportContext
    ): Boolean

    /**
     * Inserts missing rows into the database's methods and invocations tables. Does never update
     * existing rows.
     *
     * @param data The common publication data
     * @param importContext The import importContext returned by CommonImporter.importCommonData()
     * @param entries The collection of code base entries to store.
     */
    fun importMethods(
        data: CommonPublicationData2,
        importContext: CommonImporter.ImportContext,
        entries: Collection<CodeBaseEntry3>
    )

    /**
     * Upserts rows into the invocations table. Existing rows are updated with the new
     * interval.
     *
     * @param importContext The import importContext returned by CommonImporter.importCommonData()
     * @param recordingIntervalStartedAtMillis When was the invocation of these invocations recorded
     * @param invocations The set of signatures that were invoked in this recording interval.
     */
    fun importInvocations(
        importContext: CommonImporter.ImportContext,
        recordingIntervalStartedAtMillis: Long,
        invocations: Set<String>
    )

    /**
     * Upserts a row in application_descriptors
     *
     * @param data The uploaded common data
     * @param appId The applicationId
     * @param environmentId the environmentId
     */
    fun upsertApplicationDescriptor(data: CommonPublicationData2, appId: Long, environmentId: Long)
}
