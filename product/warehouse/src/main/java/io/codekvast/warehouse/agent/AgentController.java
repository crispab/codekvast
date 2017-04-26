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

import io.codekvast.agent.lib.model.Endpoints;
import io.codekvast.agent.lib.model.v1.rest.GetConfigRequest1;
import io.codekvast.agent.lib.model.v1.rest.GetConfigResponse1;
import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * The codekvast-agent REST controller.
 *
 * @author olle.hallin@crisp.se
 */
@RestController
@RequestMapping(method = POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Slf4j
public class AgentController {

    private final AgentService agentService;
    private final CodekvastSettings settings;

    @Inject
    public AgentController(AgentService agentService, CodekvastSettings settings) {
        this.agentService = agentService;
        this.settings = settings;
    }

    @ExceptionHandler
    public ResponseEntity<String> onConstraintValidationException(ConstraintViolationException e) {
        StringBuilder violations = new StringBuilder();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            violations.append(violation.getMessage()).append("\n");
        }
        log.warn("Invalid request: {}", violations);
        return ResponseEntity.badRequest().body(violations.toString());
    }

    @ExceptionHandler
    public ResponseEntity<String> onLicenseViolationException(LicenseViolationException e) {
        log.warn("Rejected request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @RequestMapping(value = Endpoints.AGENT_V1_POLL_CONFIG)
    public GetConfigResponse1 getConfig1(@Valid @RequestBody GetConfigRequest1 request) {
        log.debug("Received {}", request);

        GetConfigResponse1 response = agentService.getConfig(request);

        log.debug("Responds with {}", response);
        return response;
    }

    @RequestMapping(value = Endpoints.AGENT_V1_UPLOAD_CODEBASE, method = POST,
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String uploadCodeBase1(@RequestParam(Endpoints.AGENT_V1_UPLOAD_CODEBASE_FILE_PARAM) MultipartFile file) {
        log.debug("Received {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        // TODO save the file in the import area

        return "OK";
    }
}

