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
package io.codekvast.dashboard.agent;

import io.codekvast.dashboard.customer.LicenseViolationException;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;

import static io.codekvast.dashboard.agent.AgentService.PublicationType.CODEBASE;
import static io.codekvast.dashboard.agent.AgentService.PublicationType.INVOCATIONS;
import static io.codekvast.dashboard.util.LoggingUtils.humanReadableByteCount;
import static io.codekvast.javaagent.model.Endpoints.Agent.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * The codekvast-javaagent REST controller.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("SameReturnValue")
@RestController
@RequiredArgsConstructor
@RequestMapping(method = POST, consumes = APPLICATION_JSON_UTF8_VALUE, produces = APPLICATION_JSON_UTF8_VALUE)
@Slf4j
public class AgentController {

    private final AgentService agentService;

    @ExceptionHandler
    public ResponseEntity<String> onLicenseViolationException(LicenseViolationException e) {
        logger.warn("Rejected request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @RequestMapping(value = V1_POLL_CONFIG)
    public GetConfigResponse1 getConfig1(@Valid @RequestBody GetConfigRequest1 request) {
        logger.debug("Received {}", request);

        GetConfigResponse1 response = agentService.getConfig(request);

        logger.debug("Responds with {}", response);
        return response;
    }

    @RequestMapping(value = V1_UPLOAD_CODEBASE, method = POST,
        consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
    public String uploadCodeBase1(
        @RequestParam(PARAM_LICENSE_KEY) String licenseKey,
        @RequestParam(PARAM_FINGERPRINT) String fingerprint,
        @RequestParam(PARAM_PUBLICATION_SIZE) Integer publicationSize,
        @RequestParam(PARAM_PUBLICATION_FILE) MultipartFile file) throws IOException {

        saveUploadedPublication(CODEBASE, licenseKey, fingerprint, publicationSize, file);

        return "OK";
    }

    @RequestMapping(value = V2_UPLOAD_CODEBASE, method = POST,
        consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
    public String uploadCodeBase2(
        @RequestParam(PARAM_LICENSE_KEY) String licenseKey,
        @RequestParam(PARAM_FINGERPRINT) String fingerprint,
        @RequestParam(PARAM_PUBLICATION_SIZE) Integer publicationSize,
        @RequestParam(PARAM_PUBLICATION_FILE) MultipartFile file) throws IOException {

        saveUploadedPublication(CODEBASE, licenseKey, fingerprint, publicationSize, file);

        return "OK";
    }

    @RequestMapping(value = V1_UPLOAD_INVOCATION_DATA, method = POST,
        consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
    public String uploadInvocationData1(
        @RequestParam(PARAM_LICENSE_KEY) String licenseKey,
        @RequestParam(PARAM_FINGERPRINT) String fingerprint,
        @RequestParam(PARAM_PUBLICATION_SIZE) Integer publicationSize,
        @RequestParam(PARAM_PUBLICATION_FILE) MultipartFile file) throws IOException {

        saveUploadedPublication(INVOCATIONS, licenseKey, fingerprint, publicationSize, file);

        return "OK";
    }

    @RequestMapping(value = V2_UPLOAD_INVOCATION_DATA, method = POST,
        consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
    public String uploadInvocationData2(
        @RequestParam(PARAM_LICENSE_KEY) String licenseKey,
        @RequestParam(PARAM_FINGERPRINT) String fingerprint,
        @RequestParam(PARAM_PUBLICATION_SIZE) Integer publicationSize,
        @RequestParam(PARAM_PUBLICATION_FILE) MultipartFile file) throws IOException {

        saveUploadedPublication(INVOCATIONS, licenseKey, fingerprint, publicationSize, file);

        return "OK";
    }

    private void saveUploadedPublication(AgentService.PublicationType publicationType, String licenseKey, String fingerprint,
                                         Integer publicationSize, MultipartFile file) throws IOException {

        logger.debug("Received {} ({} {}, {}) with licenseKey={}, fingerprint={}",
                     file.getOriginalFilename(), publicationSize, publicationType,
                     humanReadableByteCount(file.getSize()), licenseKey, fingerprint);

        agentService.savePublication(publicationType, licenseKey, publicationSize, file.getInputStream());
    }

}

