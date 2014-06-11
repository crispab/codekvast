package se.crisp.duck.server.duck_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import se.crisp.duck.server.agent.AgentRestEndpoints;
import se.crisp.duck.server.agent.model.v1.SensorData;
import se.crisp.duck.server.agent.model.v1.SignatureData;
import se.crisp.duck.server.agent.model.v1.UsageData;
import se.crisp.duck.server.duck_server.service.AgentService;

import javax.inject.Inject;
import javax.validation.Valid;

/**
 * @author Olle Hallin
 */
@RestController
@Slf4j
public class AgentController {

    private final AgentService agentService;
    private final Validator validator;

    @Inject
    public AgentController(AgentService agentService, Validator validator) {
        this.agentService = agentService;
        this.validator = validator;
    }

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.setValidator(validator);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_SENSOR_V1, method = RequestMethod.POST)
    public void receiveSensorV1(@RequestBody @Valid SensorData data) {
        log.info("Received {}", data);
        agentService.storeSensorData(data);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_SIGNATURES_V1, method = RequestMethod.POST)
    public void receiveSignaturesV1(@RequestBody @Valid SignatureData data) {
        log.info("Received {}", data);
        agentService.storeSignatureData(data);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_USAGE_V1, method = RequestMethod.POST)
    public void receiveUsageV1(@RequestBody @Valid UsageData data) {
        log.info("Received {}", data);
        agentService.storeUsageData(data);
    }
}
