package se.crisp.duck.server.duck_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import se.crisp.duck.server.agent.AgentRestEndpoints;
import se.crisp.duck.server.agent.model.SignatureData;

/**
 * @author Olle Hallin
 */
@RestController
@Slf4j
public class AgentController {

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_SIGNATURES,
                    method = RequestMethod.POST,
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public void receiveSignatures(@RequestBody SignatureData data) {
        log.info("Received {}", data);
    }
}
