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
package io.codekvast.warehouse.webapp;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.security.SecurityConfig;
import io.codekvast.warehouse.security.WebappTokenProvider;
import io.codekvast.warehouse.webapp.model.methods.GetMethodsRequest1;
import io.codekvast.warehouse.webapp.model.methods.GetMethodsResponse1;
import io.codekvast.warehouse.webapp.model.methods.MethodDescriptor1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * The Webapp REST controller.
 *
 * @author olle.hallin@crisp.se
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Slf4j
public class WebappController {

    private static final String WEBAPP_V1_METHODS = "/webapp/v1/methods";
    private static final String WEBAPP_V1_METHOD = "/webapp/v1/method/detail/{id}";
    private static final String WEBAPP_RENEW_AUTH_TOKEN = "/webapp/renewAuthToken";

    private static final String X_CODEKVAST_AUTH_TOKEN = "X-Codekvast-Auth-Token";

    private final WebappService webappService;
    private final WebappTokenProvider securityHandler;
    private final CodekvastSettings settings;

    @Inject
    public WebappController(WebappService webappService, WebappTokenProvider securityHandler,
                            CodekvastSettings settings) {
        this.webappService = webappService;
        this.securityHandler = securityHandler;
        this.settings = settings;
    }

    @ExceptionHandler
    public ResponseEntity<String> onConstraintValidationException(ConstraintViolationException e) {
        StringBuilder violations = new StringBuilder();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            violations.append(violation.getMessage()).append("\n");
        }
        return ResponseEntity.badRequest().body(violations.toString());
    }

    @RequestMapping(method = GET, value = WEBAPP_V1_METHODS)
    @CrossOrigin(origins = "http://localhost:8088")
    public ResponseEntity<GetMethodsResponse1> getMethods1(@RequestParam(value = "signature", defaultValue = "%") String signature,
                                                           @RequestParam(name = "maxResults", defaultValue = WebappService
                                                               .DEFAULT_MAX_RESULTS_STR)
                                                               Integer maxResults) {
        return ResponseEntity.ok()
                             .header(X_CODEKVAST_AUTH_TOKEN, securityHandler.renewWebappToken())
                             .body(doGetMethods(signature, maxResults));
    }

    @RequestMapping(method = GET, value = WEBAPP_V1_METHOD)
    @CrossOrigin(origins = "http://localhost:8088")
    public ResponseEntity<MethodDescriptor1> getMethod1(@PathVariable(value = "id") Long methodId) {
        long startedAt = System.currentTimeMillis();

        Optional<MethodDescriptor1> result = webappService.getMethodById(methodId);

        log.debug("{} method with id={} in {} ms", result.map(methodDescriptor1 -> "Found").orElse("Could not find"),
                  methodId, System.currentTimeMillis() - startedAt);

        return result.map(method -> ResponseEntity.ok()
                                                  .header(X_CODEKVAST_AUTH_TOKEN, securityHandler.renewWebappToken())
                                                  .body(method))
                     .orElseGet(() -> ResponseEntity.notFound()
                                                    .header(X_CODEKVAST_AUTH_TOKEN, securityHandler.renewWebappToken())
                                                    .build());
    }

    @RequestMapping(method = GET, value = WEBAPP_RENEW_AUTH_TOKEN, produces = MediaType.TEXT_PLAIN_VALUE)
    @CrossOrigin(origins = "http://localhost:8088")
    public ResponseEntity<String> renewAuthToken() {
        log.debug("Renewing auth token for {}", SecurityContextHolder.getContext().getAuthentication());

        return ResponseEntity.ok()
                             .header(X_CODEKVAST_AUTH_TOKEN, securityHandler.renewWebappToken())
                             .body("OK");
    }

    @RequestMapping(method = GET, value = SecurityConfig.REQUEST_MAPPING_WEBAPP_IS_DEMO_MODE, produces = MediaType.TEXT_PLAIN_VALUE)
    @CrossOrigin(origins = "http://localhost:8088")
    public ResponseEntity<String> isDemoMode() {
        log.trace("Is demo mode? {}", settings.isDemoMode());

        return ResponseEntity.ok(Boolean.toString(settings.isDemoMode()));
    }

    private GetMethodsResponse1 doGetMethods(String signature, Integer maxResults) {
        GetMethodsRequest1 request = GetMethodsRequest1.defaults().toBuilder().signature(signature).maxResults(maxResults).build();

        GetMethodsResponse1 response = webappService.getMethods(request);

        log.debug("Response: {}", response);
        return response;
    }

}
