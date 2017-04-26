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
package io.codekvast.warehouse.agent;

import io.codekvast.agent.lib.model.v1.CodeBasePublication;
import io.codekvast.agent.lib.model.v1.InvocationDataPublication;
import io.codekvast.agent.lib.model.v1.rest.GetConfigRequest1;
import io.codekvast.agent.lib.model.v1.rest.GetConfigResponse1;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles requests from the {@link AgentController}.
 */
public interface AgentService {

    /**
     * What config parameters should this agent use?
     *
     * @param request The request object
     * @return Does never return null
     * @throws LicenseViolationException when license is violated
     */
    GetConfigResponse1 getConfig(GetConfigRequest1 request) throws LicenseViolationException;

    /**
     * Save an uploaded {@link CodeBasePublication} into the import area where it will be processed by another thread.
     *
     * @param licenseKey       The agent's licenseKey
     * @param inputStream      The data input stream.
     * @return The resulting file
     * @throws LicenseViolationException If invalid license or license violations
     */
    File saveCodeBasePublication(String licenseKey, InputStream inputStream)
        throws LicenseViolationException, IOException;

    /**
     * Save an uploaded {@link InvocationDataPublication} into the import area where it will be processed by another thread.
     *
     * @param licenseKey       The agent's licenseKey
     * @param inputStream      The data input stream.
     * @return The resulting file
     * @throws LicenseViolationException If invalid license or license violations
     */
    File saveInvocationDataPublication(String licenseKey, InputStream inputStream)
        throws LicenseViolationException, IOException;
}
