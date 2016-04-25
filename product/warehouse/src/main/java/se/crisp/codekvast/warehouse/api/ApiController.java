package se.crisp.codekvast.warehouse.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.warehouse.api.response.MethodDescriptor1;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;

import static se.crisp.codekvast.warehouse.api.ApiService.Default.MAX_RESULTS_STR;

/**
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

    @RequestMapping(value = "/api/v1/signatures", method = RequestMethod.GET)
    public List<MethodDescriptor1> describeSignature1(
            @RequestParam(name = "maxResults", defaultValue = MAX_RESULTS_STR) Integer maxResults) {

        List<MethodDescriptor1> result = apiService.describeSignature1(
                DescribeSignature1Parameters.defaults()
                                            .signature("%")
                                            .maxResults(maxResults)
                                            .build());

        log.debug("Result: {} methods", result.size());
        return result;
    }

    @RequestMapping(value = "/api/v1/signatures/{signature}", method = RequestMethod.GET)
    public List<MethodDescriptor1> describeSignature1(@PathVariable("signature") String signature,
                                                      @RequestParam(name = "maxResults", defaultValue = MAX_RESULTS_STR) Integer maxResults) {

        log.debug("signature={}", signature);

        List<MethodDescriptor1> result = apiService.describeSignature1(
                DescribeSignature1Parameters.defaults()
                                            .signature(signature)
                                            .maxResults(maxResults)
                                            .build());

        log.debug("Result: {} methods", result.size());
        return result;
    }
}
