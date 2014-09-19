package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.server.agent.AgentRestEndpoints;
import se.crisp.codekvast.server.agent.model.v1.SensorRunData;
import se.crisp.codekvast.server.agent.model.v1.SignatureData;
import se.crisp.codekvast.server.agent.model.v1.UsageData;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;
import javax.validation.Valid;

/**
 * A HTTP REST Controller that handles requests from the CodeKvast Agent.
 * <p/>
 * It validates the POST data and delegates to AgentService.
 *
 * @author Olle Hallin
 */
@RestController
@Slf4j
public class AgentController {

    private final StorageService storageService;
    private final Validator validator;

    @Inject
    public AgentController(StorageService storageService, Validator validator) {
        this.storageService = storageService;
        this.validator = validator;
    }

    @InitBinder
    private void initBinder(WebDataBinder binder) {
        binder.setValidator(validator);
    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    private void onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Bad request: " + e);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_SENSOR_RUN_V1, method = RequestMethod.POST)
    public void receiveSensorV1(@RequestBody @Valid SensorRunData data) {
        log.debug("Received {}", data);
        storageService.storeSensorData(data);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_SIGNATURES_V1, method = RequestMethod.POST)
    public void receiveSignaturesV1(@RequestBody @Valid SignatureData data) {
        log.debug("Received {}", data);
        storageService.storeSignatureData(data);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_USAGE_V1, method = RequestMethod.POST)
    public void receiveUsageV1(@RequestBody @Valid UsageData data) {
        log.debug("Received {}", data);
        storageService.storeUsageData(data);
    }
}
