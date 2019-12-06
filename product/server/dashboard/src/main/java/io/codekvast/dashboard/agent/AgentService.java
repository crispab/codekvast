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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
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
import io.codekvast.javaagent.model.v2.GetConfigRequest2;
import io.codekvast.javaagent.model.v2.GetConfigResponse2;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import io.codekvast.javaagent.model.v3.CodeBasePublication3;

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
     * What config parameters should this javaagent use?
     *
     * @param request The request object
     * @return Does never return null
     * @throws LicenseViolationException when license is violated
     */
    GetConfigResponse2 getConfig(GetConfigRequest2 request) throws LicenseViolationException;

    /**
     * Save an uploaded publication into the import area where it will be processed by another thread.
     *
     * @param publicationType     The type of publication.
     * @param licenseKey          The javaagent's licenseKey.
     * @param codebaseFingerprint The publication's origin codebase's fingerprint
     * @param publicationSize     The size of the publication. Used for price plan enforcement.
     * @param inputStream         The data input stream.  @return The resulting file or null of the code base was already uploaded.
     * @return the resulting file in the queue directory. The filename is made up of the publicationType, the customer ID and the
     * correlationId.
     * @throws LicenseViolationException If invalid license or license violations.
     * @throws IOException               If failure to create the file.
     * @see CodeBasePublication2
     * @see CodeBasePublication3
     * @see InvocationDataPublication2
     * @see #getCorrelationIdFromPublicationFile(File)
     */
    File savePublication(PublicationType publicationType, String licenseKey, String codebaseFingerprint, int publicationSize,
                         InputStream inputStream)
        throws LicenseViolationException, IOException;

    /**
     * Generates a file name from the supplied parameters.
     *
     * @param publicationType The type of publication.
     * @param customerId      The customer ID.
     * @param correlationId   The correlation ID.
     * @return A filename to use when saving a publication file. The generated file name can be parsed by
     * {@link #getCorrelationIdFromPublicationFile(File)}.
     */
    File generatePublicationFile(PublicationType publicationType, Long customerId, String correlationId);

    /**
     * Retrieve the correlationId from a file name generated by {@link #generatePublicationFileName(PublicationType, Long, String)}.
     *
     * @param publicationFile A file created by {@link #savePublication(PublicationType, String, String, int, InputStream)}.
     * @return The correlationId part of the file name.
     */
    String getCorrelationIdFromPublicationFile(File publicationFile);
}
