/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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
package io.codekvast.dashboard.agent;

import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.model.v2.CodeBasePublication2;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles requests from the {@link AgentController}.
 */
public interface AgentService {

    enum PublicationType {
        CODEBASE, INVOCATIONS;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /**
     * What config parameters should this javaagent use?
     *
     * @param request The request object
     * @return Does never return null
     * @throws LicenseViolationException when license is violated
     */
    GetConfigResponse1 getConfig(GetConfigRequest1 request) throws LicenseViolationException;

    /**
     * Save an uploaded publication into the import area where it will be processed by another thread.
     *
     * @param publicationType The type of publication.
     * @param licenseKey      The javaagent's licenseKey.
     * @param publicationSize The size of the publication. Used for price plan enforcement.
     * @param inputStream     The data input stream.  @return The resulting file or null of the code base was already uploaded.
     * @return the resulting file in the queue directory.
     * @throws LicenseViolationException If invalid license or license violations.
     * @throws IOException               If failure to create the file.
     * @see CodeBasePublication2
     * @see InvocationDataPublication2
     */
    File savePublication(PublicationType publicationType, String licenseKey, int publicationSize, InputStream inputStream)
        throws LicenseViolationException, IOException;
}
