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
package se.crisp.codekvast.warehouse.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.warehouse.api.model.GetMethodsRequest1;
import se.crisp.codekvast.warehouse.api.model.GetMethodsResponse1;
import se.crisp.codekvast.warehouse.api.model.MethodDescriptor1;
import se.crisp.codekvast.warehouse.bootstrap.CodekvastSettings;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static se.crisp.codekvast.warehouse.api.ApiService.DEFAULT_MAX_RESULTS_STR;

/**
 * The API REST controller.
 *
 * @author olle.hallin@crisp.se
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Slf4j
public class ApiController {

    private static final String API_V1_METHODS = "/api/v1/methods";
    private final ApiService apiService;
    private final CodekvastSettings settings;

    @Inject
    public ApiController(ApiService apiService, CodekvastSettings settings) {
        this.apiService = apiService;
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

    @RequestMapping(method = GET, value = API_V1_METHODS)
    @CrossOrigin(origins = "http://localhost:8088")
    public ResponseEntity<GetMethodsResponse1> getMethods1(HttpServletRequest request,
                                                           @RequestParam(value = "signature", defaultValue = "%") String signature,
                                                           @RequestParam(name = "maxResults", defaultValue = DEFAULT_MAX_RESULTS_STR)
                                                                       Integer maxResults) {
        return ResponseEntity.ok().body(doGetMethods(signature, maxResults));
    }

    private GetMethodsResponse1 doGetMethods(String signature, Integer maxResults) {
        long startedAt = System.currentTimeMillis();

        GetMethodsRequest1 request = GetMethodsRequest1.defaults().toBuilder().signature(signature).maxResults(maxResults).build();

        List<MethodDescriptor1> methods = apiService.getMethods(request);

        GetMethodsResponse1 response = GetMethodsResponse1.builder()
                                                          .timestamp(startedAt)
                                                          .request(request)
                                                          .queryTimeMillis(System.currentTimeMillis() - startedAt)
                                                          .numMethods(methods.size())
                                                          .methods(methods)
                                                          .build();
        log.debug("Response: {}", response);
        return response;
    }

    @RequestMapping(method = GET, value = "/api/instant")
    public Instant getInstant() {
        return Instant.now();
    }

    @RequestMapping(method = GET, value = "/api/localDateTime")
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.now();
    }

    @RequestMapping(method = GET, value = "/api/localDateTime/iso")
    public String getLocalDateTimeString(Locale locale) {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale(locale);
        return LocalDateTime.now().format(dtf);
    }

    @RequestMapping(method = GET, value = "/api/version")
    public CodekvastSettings getVersion() {
        return settings;
    }
}
