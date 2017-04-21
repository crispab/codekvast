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
package se.crisp.codekvast.warehouse.webapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.warehouse.bootstrap.CodekvastSettings;
import se.crisp.codekvast.warehouse.webapp.model.GetMethodsRequest1;
import se.crisp.codekvast.warehouse.webapp.model.GetMethodsResponse1;
import se.crisp.codekvast.warehouse.webapp.model.MethodDescriptor1;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static se.crisp.codekvast.warehouse.webapp.WebappService.DEFAULT_MAX_RESULTS_STR;

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

    private final WebappService webappService;
    private final CodekvastSettings settings;

    @Inject
    public WebappController(WebappService webappService, CodekvastSettings settings) {
        this.webappService = webappService;
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
                                                           @RequestParam(name = "maxResults", defaultValue = DEFAULT_MAX_RESULTS_STR)
                                                               Integer maxResults) {
        return ResponseEntity.ok().body(doGetMethods(signature, maxResults));
    }

    @RequestMapping(method = GET, value = WEBAPP_V1_METHOD)
    @CrossOrigin(origins = "http://localhost:8088")
    public ResponseEntity<MethodDescriptor1> getMethod1(@PathVariable(value = "id") Long methodId) {
        long startedAt = System.currentTimeMillis();

        Optional<MethodDescriptor1> result = webappService.getMethodById(methodId);

        log.debug("{} method with id={} in {} ms", result.isPresent() ? "Found" : "Could not find", methodId, System.currentTimeMillis() - startedAt);

        return result.map(method -> ResponseEntity.ok().body(method))
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private GetMethodsResponse1 doGetMethods(String signature, Integer maxResults) {
        long startedAt = System.currentTimeMillis();

        GetMethodsRequest1 request = GetMethodsRequest1.defaults().toBuilder().signature(signature).maxResults(maxResults).build();

        List<MethodDescriptor1> methods = webappService.getMethods(request);

        GetMethodsResponse1 response = GetMethodsResponse1.builder()
                                                          .timestamp(startedAt)
                                                          .request(request)
                                                          .numMethods(methods.size())
                                                          .methods(methods)
                                                          .queryTimeMillis(System.currentTimeMillis() - startedAt)
                                                          .build();
        log.debug("Response: {}", response);
        return response;
    }

    // Experimental stuff below

    @RequestMapping(method = GET, value = "/server/instant")
    public Instant getInstant() {
        return Instant.now();
    }

    @RequestMapping(method = GET, value = "/server/localDateTime")
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.now();
    }

    @RequestMapping(method = GET, value = "/server/localDateTime/iso")
    public String getLocalDateTimeString(Locale locale) {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale(locale);
        return LocalDateTime.now().format(dtf);
    }

    @RequestMapping(method = GET, value = "/server/version")
    public CodekvastSettings getVersion() {
        return settings;
    }
}
