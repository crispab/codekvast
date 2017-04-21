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
package se.crisp.codekvast.warehouse.agent.impl;

import org.springframework.stereotype.Service;
import se.crisp.codekvast.agent.lib.model.rest.GetConfigRequest1;
import se.crisp.codekvast.agent.lib.model.rest.GetConfigResponse1;
import se.crisp.codekvast.warehouse.agent.AgentService;
import se.crisp.codekvast.warehouse.agent.LicenseViolationException;

/**
 * Handler for agent REST requests.
 *
 * @author Olle Hallin &lt;olle.hallin@crisp.se&gt;
 */
@Service
public class AgentServiceImpl implements AgentService {
    @Override
    public GetConfigResponse1 getConfig(GetConfigRequest1 request) throws LicenseViolationException {
        checkLicense(request);
        return GetConfigResponse1.builder()
                                 .codeBasePublisherClass("no-op")
                                 .codeBasePublisherConfig("enabled=true")
                                 .build();
    }

    private void checkLicense(GetConfigRequest1 request) {
        // TODO: implement proper license control
        if (request.getLicenseKey().equals("-----")) {
            throw new LicenseViolationException("Invalid license key: " + request.getLicenseKey());
        }
    }
}
