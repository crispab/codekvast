package se.crisp.codekvast.warehouse.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.warehouse.api.response.MethodDescriptor1;

import javax.inject.Inject;
import java.util.List;

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

    @RequestMapping(value = "/api/v1/describe/{signature}", method = RequestMethod.GET)
    public List<MethodDescriptor1> describeSignature1(@PathVariable("signature") String signature,
                                                      @RequestParam(name = "maxResults", required = false,
                                                              defaultValue = ApiService.Default.MAX_RESULTS_STR)
                                                              Integer maxResults) {

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
