package se.crisp.codekvast.warehouse.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.crisp.codekvast.warehouse.api.model.MethodDescriptor;

import javax.inject.Inject;
import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
@RestController
@Slf4j
public class QueryController {

    private final QueryService queryService;

    @Inject
    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @RequestMapping("/api/query")
    public List<MethodDescriptor> getMethods(@RequestParam("signature")
                                                     String signature,
                                             @RequestParam(name = "maxResults", required = false, defaultValue = QueryService.Default
                                                     .MAX_RESULTS_STR)
                                                     Integer maxResults) {
        log.debug("signature={}", signature);

        List<MethodDescriptor> result = queryService.queryMethodsBySignature(
                QueryMethodsBySignatureParameters.defaults()
                                                 .signature(signature)
                                                 .maxResults(maxResults)
                                                 .build());

        log.debug("Result: {} methods", result.size());
        return result;
    }
}
