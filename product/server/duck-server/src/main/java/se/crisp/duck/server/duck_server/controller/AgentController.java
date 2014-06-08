package se.crisp.duck.server.duck_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import se.crisp.duck.server.agent.AgentRestEndpoints;
import se.crisp.duck.server.agent.model.UploadSignatureData;

/**
 * @author Olle Hallin
 */
@RestController
@Slf4j
public class AgentController {

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_SIGNATURES, method = RequestMethod.POST)
    public void receiveSignatures(@RequestBody UploadSignatureData data) {
        log.info("Received {}", data);
    }
}
