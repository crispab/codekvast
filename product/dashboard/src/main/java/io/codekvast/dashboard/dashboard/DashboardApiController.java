/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
package io.codekvast.dashboard.dashboard;

import io.codekvast.dashboard.dashboard.model.methods.GetMethodsRequest;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsResponse;
import io.codekvast.dashboard.dashboard.model.methods.MethodDescriptor;
import io.codekvast.dashboard.dashboard.model.status.GetStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
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
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboardService;

    @ExceptionHandler
    public ResponseEntity<String> onConstraintValidationException(ConstraintViolationException e) {
        StringBuilder violations = new StringBuilder();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            violations.append(violation.getMessage()).append("\n");
        }
        return ResponseEntity.badRequest().body(violations.toString());
    }

    @RequestMapping(method = GET, path = "/dashboard/api/v1/methods")
    public ResponseEntity<GetMethodsResponse> getMethods1(
        @RequestParam(value = "signature", defaultValue = "%") String signature,
        @RequestParam(value = "onlyInvokedBeforeMillis", defaultValue = DashboardService.DEFAULT_ONLY_INVOKED_BEFORE_MILLIS_STR) Long
            onlyInvokedBeforeMillis,
        @RequestParam(value = "suppressSyntheticMethods", defaultValue = DashboardService.DEFAULT_SUPPRESS_SYNTHETIC_METHODS_STR) Boolean
            suppressSyntheticMethods,
        @RequestParam(value = "suppressUntrackedMethods", defaultValue = DashboardService.DEFAULT_SUPPRESS_UNTRACKED_METHODS_STR) Boolean
            suppressUntrackedMethods,
        @RequestParam(name = "minCollectedDays", defaultValue = DashboardService.DEFAULT_MIN_COLLECTED_DAYS_STR) Integer minCollectedDays,
        @RequestParam(name = "maxResults", defaultValue = DashboardService.DEFAULT_MAX_RESULTS_STR) Integer maxResults) {

        GetMethodsRequest request = GetMethodsRequest.defaults().toBuilder()
                                                     .signature(signature)
                                                     .onlyInvokedBeforeMillis(onlyInvokedBeforeMillis)
                                                     .suppressUntrackedMethods(suppressUntrackedMethods)
                                                     .suppressSyntheticMethods(suppressSyntheticMethods)
                                                     .minCollectedDays(minCollectedDays)
                                                     .maxResults(maxResults)
                                                     .build();

        return ResponseEntity.ok().body(doGetMethods(request));
    }

    @RequestMapping(method = GET, path = "/dashboard/api/v1/method/detail/{id}")
    public ResponseEntity<MethodDescriptor> getMethod1(@PathVariable(value = "id") Long methodId) {
        long startedAt = System.currentTimeMillis();

        Optional<MethodDescriptor> result = dashboardService.getMethodById(methodId);

        logger.debug("{} method with id={} in {} ms", result.map(methodDescriptor1 -> "Found").orElse("Could not find"),
                     methodId, System.currentTimeMillis() - startedAt);

        return result.map(method -> ResponseEntity.ok().body(method))
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @RequestMapping(method = GET, path = "/dashboard/api/v1/status")
    public ResponseEntity<GetStatusResponse> getStatus1() {
        GetStatusResponse response = dashboardService.getStatus();
        logger.debug("Response: {}", response);
        return ResponseEntity.ok().body(response);
    }

    private GetMethodsResponse doGetMethods(GetMethodsRequest request) {
        logger.debug("Request: {}", request);
        GetMethodsResponse response = dashboardService.getMethods(request);
        logger.debug("Response: {}", response);
        return response;
    }

}
