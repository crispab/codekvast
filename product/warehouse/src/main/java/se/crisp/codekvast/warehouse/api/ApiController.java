package se.crisp.codekvast.warehouse.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.crisp.codekvast.warehouse.api.model.GetMethodsRequest1;
import se.crisp.codekvast.warehouse.api.model.GetMethodsResponse1;
import se.crisp.codekvast.warehouse.api.model.MethodDescriptor1;
import se.crisp.codekvast.warehouse.bootstrap.CodekvastSettings;

import javax.inject.Inject;
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
@RequestMapping(value = "/api/", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Slf4j
public class ApiController {

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

    @RequestMapping(method = GET, value = "/v1/methods")
    public GetMethodsResponse1 getMethods1(@RequestParam(value = "signature", defaultValue = "%") String signature,
                                           @RequestParam(name = "maxResults", defaultValue = DEFAULT_MAX_RESULTS_STR) Integer maxResults) {
        return doGetMethods(signature, maxResults);
    }

    private GetMethodsResponse1 doGetMethods(String signature, Integer maxResults) {
        long startedAt = System.currentTimeMillis();

        GetMethodsRequest1 request = GetMethodsRequest1.defaults().signature(signature).maxResults(maxResults).build();

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

    @RequestMapping(method = GET, value = "/instant")
    public Instant getInstant() {
        return Instant.now();
    }

    @RequestMapping(method = GET, value = "/localDateTime")
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.now();
    }

    @RequestMapping(method = GET, value = "/localDateTime/iso")
    public String getLocalDateTimeString(Locale locale) {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale(locale);
        return LocalDateTime.now().format(dtf);
    }

    @RequestMapping(method = GET, value = "/version")
    public CodekvastSettings getVersion() {
        return settings;
    }

}
