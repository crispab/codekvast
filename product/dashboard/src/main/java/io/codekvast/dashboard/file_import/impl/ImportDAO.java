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
package io.codekvast.dashboard.file_import.impl;

import io.codekvast.javaagent.model.v1.CodeBaseEntry1;
import io.codekvast.javaagent.model.v1.CommonPublicationData1;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Interface for importing stuff to the database.
 *
 * @author olle.hallin@crisp.se
 */
public interface ImportDAO {

    /**
     * Inserts or updates a row in the applications table.
     *
     * @param commonData The application data
     * @return The primary key of the inserted or updated applications row.
     */
    long importApplication(CommonPublicationData1 commonData);

    /**
     * Inserts or updates a row in the jvms table.
     *
     * @param commonData    The JVM data
     * @param applicationId The value returned from {@link #importApplication(CommonPublicationData1)}
     * @return The primary key of the inserted or updated jvms row.
     */
    long importJvm(CommonPublicationData1 commonData, long applicationId);

    /**
     * Inserts missing rows into the database's methods and invocations tables. Does never update existing rows.
     *
     * @param customerId        The customer ID
     * @param appId             The application ID returned by {@link #importApplication(CommonPublicationData1)}
     * @param jvmId             The JVM ID returned by {@link #importJvm(CommonPublicationData1, long)}
     * @param publishedAtMillis The timestamp the publication was published.
     * @param entries           The collection of code base entries to store.
     */
    void importMethods(long customerId, long appId, long jvmId, long publishedAtMillis, Collection<CodeBaseEntry1> entries);

    /**
     * Inserts or updates rows into the invocations table. Existing rows are updated with the new interval.
     *
     * @param customerId      The customer ID
     * @param appId           The application ID returned by {@link #importApplication(CommonPublicationData1)}
     * @param jvmId           The JVM ID returned by {@link #importJvm(CommonPublicationData1, long)}
     * @param invokedAtMillis The start of the recording interval.
     * @param invocations     The set of signatures that were invoked in this recording interval.
     */
    void importInvocations(long customerId, long appId, long jvmId, long invokedAtMillis, Set<String> invocations);

    /**
     * Inserts rows in the strange_signatures table
     *
     * @param customerId        The customer ID
     * @param appId             The application ID returned by {@link #importApplication(CommonPublicationData1)}
     * @param jvmId             The JVM ID returned by {@link #importJvm(CommonPublicationData1, long)}
     * @param strangeSignatures A Map with the raw signature as key, and the normalized signature as value
     */
    void importStrangeSignatures(long customerId, long appId, long jvmId, Map<String, String> strangeSignatures);
}
