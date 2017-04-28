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

import io.codekvast.agent.lib.model.v1.CommonPublicationData;

/**
 * Interface for importing stuff to the database.
 *
 * @author olle.hallin@crisp.se
 */
public interface ImportDAO {

    /**
     * Inserts or updates a row in the applications table.
     *
     * @param name            The name of the application.
     * @param version         The version of the application.
     * @param startedAtMillis The instant this application was started.
     * @return The primary key of the inserted or updated row.
     */
    long importApplication(String name, String version, long startedAtMillis);

    /**
     * Inserts or updates a row in the jvms table.
     *
     * @param commonData      The JVM data
     * @return The primary key of the inserted or updated row.
     */
    long importJvm(CommonPublicationData commonData);
}
