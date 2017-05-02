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
package io.codekvast.agent.api.model;

/**
 * Definition ov the HTTP endpoints offered by the Codekvast server's AgentController
 *
 * @author olle.hallin@crisp.se
 */
public interface Endpoints {
    String AGENT_V1_POLL_CONFIG = "/agent/v1/pollConfig";
    String AGENT_V1_UPLOAD_CODEBASE = "/agent/v1/uploadCodeBase";
    String AGENT_V1_UPLOAD_INVOCATION_DATA = "/agent/v1/uploadInvocationData";

    String AGENT_V1_PUBLICATION_FILE_PARAM = "publicationFile";
    String AGENT_V1_LICENSE_KEY_PARAM = "licenseKey";
    String AGENT_V1_FINGERPRINT_PARAM = "fingerprint";
}
