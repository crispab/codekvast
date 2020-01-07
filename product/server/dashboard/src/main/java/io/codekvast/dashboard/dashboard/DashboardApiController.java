/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
package io.codekvast.dashboard.dashboard;

import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.dashboard.dashboard.model.ServerSettings;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsFormData;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsRequest;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsResponse2;
import io.codekvast.dashboard.dashboard.model.methods.MethodDescriptor1;
import io.codekvast.dashboard.dashboard.model.status.GetStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import java.util.Optional;

/**
 * The Webapp REST controller.
 *
 * @author olle.hallin@crisp.se
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class DashboardApiController {

    private final DashboardService dashboardService;
    private final CodekvastDashboardSettings settings;

    @ExceptionHandler
    public ResponseEntity<String> onConstraintValidationException(ConstraintViolationException e) {
        StringBuilder violations = new StringBuilder();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            violations.append(violation.getMessage()).append("\n");
        }
        return ResponseEntity.badRequest().body(violations.toString());
    }

    @GetMapping("/dashboard/api/v1/serverSettings")
    public ServerSettings getServerSettings() {
        String displayVersion = this.settings.getDisplayVersion();

        ServerSettings serverSettings = ServerSettings.builder()
                                                      .loginUrl(this.settings.getLoginBaseUrl() + "/userinfo")
                                                      .logoutUrl(this.settings.getLoginBaseUrl() + "/userinfo")
                                                      .serverVersion(displayVersion.startsWith("<%=") ? "dev" : displayVersion)
                                                      .build();

        logger.debug("getServerSettings() returns {}", serverSettings);
        return serverSettings;
    }

    @PostMapping("/dashboard/api/v2/methods")
    public GetMethodsResponse2 getMethods2(@Valid @RequestBody GetMethodsRequest request) {
        logger.debug("Request: {}", request);
        GetMethodsResponse2 response = dashboardService.getMethods2(request);
        logger.trace("Response: {}", response);
        return response;
    }

    @GetMapping("/dashboard/api/v1/method/detail/{id}")
    public ResponseEntity<MethodDescriptor1> getMethod1(@PathVariable(value = "id") Long methodId) {
        long startedAt = System.currentTimeMillis();

        Optional<MethodDescriptor1> result = dashboardService.getMethodById(methodId);

        logger.debug("{} method with id={} in {} ms", result.map(methodDescriptor1 -> "Found").orElse("Could not find"),
                     methodId, System.currentTimeMillis() - startedAt);

        return result.map(method -> ResponseEntity.ok().body(method))
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/dashboard/api/v1/status")
    public GetStatusResponse getStatus1() {
        GetStatusResponse status = dashboardService.getStatus();
        logger.trace("{}", status);
        return status;
    }

    @GetMapping("/dashboard/api/v1/methodsFormData")
    public GetMethodsFormData getMethodsFormData() {
        GetMethodsFormData data = dashboardService.getMethodsFormData();
        logger.debug("{}", data);
        return data;
    }
}
