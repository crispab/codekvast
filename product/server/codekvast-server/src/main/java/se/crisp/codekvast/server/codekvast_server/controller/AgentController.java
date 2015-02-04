package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.server.agent_api.AgentRestEndpoints;
import se.crisp.codekvast.server.agent_api.model.test.Ping;
import se.crisp.codekvast.server.agent_api.model.test.Pong;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureData;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.inject.Inject;
import javax.validation.Valid;
import java.security.Principal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A HTTP REST Controller that handles requests from the Codekvast Agent.
 *
 * It validates the POST data and delegates to AgentService.
 *
 * @author Olle Hallin
 */
@RestController
@Slf4j
public class AgentController {

    @NonNull
    private final AgentService agentService;

    @NonNull
    private final Validator validator;

    @Inject
    public AgentController(AgentService agentService, Validator validator) {
        this.agentService = agentService;
        this.validator = validator;
    }

    @InitBinder
    private void initBinder(WebDataBinder binder) {
        binder.setValidator(checkNotNull(validator, "validator is null"));
    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.PRECONDITION_FAILED)
    private void onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation failure: " + e);
    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    private void onApplicationException(CodekvastException e) {
        log.error("Application exception: " + e);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_V1_JVM_DATA, method = RequestMethod.POST)
    public void receiveJvmDataV1(@Valid @RequestBody JvmData data, Principal principal) throws CodekvastException {
        long startedAt = System.currentTimeMillis();
        log.debug("Received {} from {}", data, principal.getName());

        agentService.storeJvmData(principal.getName(), data);

        log.info("Stored JVM info from {} {} in {} ms", data.getAppName(), data.getAppVersion(), System.currentTimeMillis() - startedAt);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_V1_SIGNATURES, method = RequestMethod.POST)
    public void receiveSignatureDataV1(@Valid @RequestBody SignatureData data, Principal principal) throws CodekvastException {
        long startedAt = System.currentTimeMillis();
        log.debug("Received {} signatures from {}", data.getSignatures().size(), principal.getName());

        agentService.storeSignatureData(data);

        log.info("Stored {} signatures in {} ms", data.getSignatures().size(), System.currentTimeMillis() - startedAt);
    }

    @RequestMapping(value = AgentRestEndpoints.PING, method = RequestMethod.POST)
    public Pong ping(@Valid @RequestBody Ping data, Principal principal) {
        log.info("Received {} from {}", data, principal.getName());
        return Pong.builder().message("You said " + data.getMessage()).build();
    }
}
