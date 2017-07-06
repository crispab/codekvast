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
package io.codekvast.warehouse.agent;

import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.warehouse.customer.LicenseViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import java.io.IOException;

import static io.codekvast.javaagent.model.Endpoints.Agent.*;
import static java.lang.String.format;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * The codekvast-javaagent REST controller.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("SameReturnValue")
@RestController
@RequestMapping(method = POST, consumes = APPLICATION_JSON_UTF8_VALUE, produces = APPLICATION_JSON_UTF8_VALUE)
@Slf4j
public class AgentController {

    private final AgentService agentService;

    @Inject
    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @ExceptionHandler
    public ResponseEntity<String> onConstraintValidationException(ConstraintViolationException e) {
        StringBuilder violations = new StringBuilder();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            violations.append(violation.getMessage()).append("\n");
        }
        logger.warn("Invalid request: {}", violations);
        return ResponseEntity.badRequest().body(violations.toString());
    }

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

        logger.debug("Received {} ({} methods, {}) with licenseKey={}, fingerprint={}", file.getOriginalFilename(),
                  humanReadableByteCount(file.getSize()), publicationSize, licenseKey, fingerprint);

        agentService.saveCodeBasePublication(licenseKey, fingerprint, publicationSize, file.getInputStream());

        return "OK";
    }

    @RequestMapping(value = V1_UPLOAD_INVOCATION_DATA, method = POST,
        consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
    public String uploadInvocationData1(
        @RequestParam(PARAM_LICENSE_KEY) String licenseKey,
        @RequestParam(PARAM_FINGERPRINT) String fingerprint,
        @RequestParam(PARAM_PUBLICATION_SIZE) Integer publicationSize,
        @RequestParam(PARAM_PUBLICATION_FILE) MultipartFile file) throws IOException {

        logger.debug("Received {} ({} methods, {}) with licenseKey={}, fingerprint={}", file.getOriginalFilename(),
                  humanReadableByteCount(file.getSize()), publicationSize, licenseKey, fingerprint);

        agentService.saveInvocationDataPublication(licenseKey, fingerprint, publicationSize, file.getInputStream());

        return "OK";
    }

    private String humanReadableByteCount(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        }
        int exponent = (int) (Math.log(bytes) / Math.log(1000));
        String unit = " kMGTPE".charAt(exponent) + "B";
        return format("%.1f %s", bytes / Math.pow(1000, exponent), unit);
    }

}

