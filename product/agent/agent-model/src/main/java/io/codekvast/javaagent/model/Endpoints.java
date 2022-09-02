/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
package io.codekvast.javaagent.model;

/**
 * Definition of the HTTP endpoints and parameters offered by the Codekvast server's AgentController
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"MarkerInterface", "InnerClassTooDeeplyNested"})
public interface Endpoints {

  interface Agent {
    String V1_POLL_CONFIG = "/javaagent/v1/pollConfig";
    String V2_POLL_CONFIG = "/javaagent/v2/pollConfig";
    String V2_UPLOAD_CODEBASE = "/javaagent/v2/uploadCodeBase";
    String V3_UPLOAD_CODEBASE = "/javaagent/v3/uploadCodeBase";
    String V2_UPLOAD_INVOCATION_DATA = "/javaagent/v2/uploadInvocationData";

    String PARAM_FINGERPRINT = "fingerprint";
    String PARAM_LICENSE_KEY = "licenseKey";
    String PARAM_PUBLICATION_SIZE = "numMethods";
    String PARAM_PUBLICATION_FILE = "publicationFile";
  }
}
