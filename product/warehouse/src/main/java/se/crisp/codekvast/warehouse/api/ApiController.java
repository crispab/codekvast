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
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
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

    /**
     * Make it possible to invoke getMethods1() from another server running in localhost (e.g., 'npm start' or debugging with IDEA in
     * Chrome...
     */
    private ResponseEntity.BodyBuilder addCorsHeaderForLocalhost(HttpServletRequest request) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        String origin = request.getHeader("Origin");
        if (origin != null && origin.contains("//localhost")) {
            builder.header("Access-Control-Allow-Origin", origin);
        }
        return builder;
    }

    // This happens when running from 'npm start'
    @RequestMapping(method = OPTIONS, value = API_V1_METHODS)
    public ResponseEntity<Void> allow_any_localhost_port_for_getMethods1(HttpServletRequest request) {
        return addCorsHeaderForLocalhost(request).build();
    }

    @RequestMapping(method = GET, value = API_V1_METHODS)
    public ResponseEntity<GetMethodsResponse1> getMethods1(HttpServletRequest request,
                                                           @RequestParam(value = "signature", defaultValue = "%") String signature,
                                                           @RequestParam(name = "maxResults", defaultValue = DEFAULT_MAX_RESULTS_STR)
                                                                       Integer maxResults) {
        return addCorsHeaderForLocalhost(request).body(doGetMethods(signature, maxResults));
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
