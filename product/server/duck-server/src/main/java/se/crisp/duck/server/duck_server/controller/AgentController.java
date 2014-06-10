package se.crisp.duck.server.duck_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import se.crisp.duck.server.agent.AgentRestEndpoints;
import se.crisp.duck.server.agent.model.v1.SensorData;
import se.crisp.duck.server.agent.model.v1.SignatureData;
import se.crisp.duck.server.agent.model.v1.UsageData;
import se.crisp.duck.server.duck_server.service.AgentService;

import javax.inject.Inject;

/**
 * @author Olle Hallin
 */
@RestController
@Slf4j
public class AgentController {

    private final AgentService agentService;

    @Inject
    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_SENSOR_V1, method = RequestMethod.POST)
    public void receiveSensorV1(@RequestBody SensorData data) {
        log.info("Received {}", data);
        agentService.storeSensorData(data);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_SIGNATURES_V1, method = RequestMethod.POST)
    public void receiveSignaturesV1(@RequestBody SignatureData data) {
        log.info("Received {}", data);
        agentService.storeSignatureData(data);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_USAGE_V1, method = RequestMethod.POST)
    public void receiveUsageV1(@RequestBody UsageData data) {
        log.info("Received {}", data);
        agentService.storeUsageData(data);
    }
}
