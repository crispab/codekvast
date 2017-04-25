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
package io.codekvast.warehouse.agent.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import io.codekvast.agent.lib.model.rest.GetConfigRequest1;
import io.codekvast.agent.lib.model.rest.GetConfigResponse1;
import io.codekvast.warehouse.agent.AgentService;
import io.codekvast.warehouse.agent.LicenseViolationException;

import java.util.HashSet;
import java.util.Set;

/**
 * Handler for agent REST requests.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final Set<String> codeBaseFingerprints = new HashSet<>();

    @Override
    public GetConfigResponse1 getConfig(GetConfigRequest1 request) throws LicenseViolationException {
        checkLicense(request);

        // TODO: store code base fingerprint in database
        boolean codeBaseAdded = codeBaseFingerprints.add(request.getCodeBaseFingerprint());

        return GetConfigResponse1.builder()
                                 .codeBasePublisherName("no-op")
                                 .codeBasePublisherConfig("enabled=true")
                                 .codeBasePublishingNeeded(codeBaseAdded)
                                 .invocationDataPublisherName("file-system")
                                 .invocationDataPublisherConfig("enabled=true")
                                 .build();
    }

    private void checkLicense(GetConfigRequest1 request) {
        // TODO: implement proper license control
        if ("-----".equals(request.getLicenseKey())) {
            throw new LicenseViolationException("Invalid license key: " + request.getLicenseKey());
        }

        if (request.getLicenseKey() == null || request.getLicenseKey().trim().isEmpty()) {
            log.debug("Running without a license.");
        }
    }
}
