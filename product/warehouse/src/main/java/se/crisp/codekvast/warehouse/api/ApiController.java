package se.crisp.codekvast.warehouse.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.warehouse.api.model.GetMethodsRequest1;
import se.crisp.codekvast.warehouse.api.model.GetMethodsResponse1;
import se.crisp.codekvast.warehouse.api.model.MethodDescriptor1;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;

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

    private final ApiService apiService;

    @Inject
    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @ExceptionHandler
    public ResponseEntity<String> onConstraintValidationException(ConstraintViolationException e) {
        StringBuilder violations = new StringBuilder();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            violations.append(violation.getMessage()).append("\n");
        }
        return ResponseEntity.badRequest().body(violations.toString());
    }

    @RequestMapping(method = GET, value = "/api/v1/methods")
    public GetMethodsResponse1 getMethods1(@RequestParam(name = "maxResults", defaultValue = DEFAULT_MAX_RESULTS_STR) Integer maxResults) {

        return doGetMethods("%", maxResults);
    }

    @RequestMapping(method = GET, value = "/api/v1/methods/{signature}")
    public GetMethodsResponse1 getMethods1(@PathVariable("signature") String signature,
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

}
