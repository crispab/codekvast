package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.server.agent.AgentRestEndpoints;
import se.crisp.codekvast.server.agent.model.test.Ping;
import se.crisp.codekvast.server.agent.model.test.Pong;
import se.crisp.codekvast.server.agent.model.v1.JvmRunData;
import se.crisp.codekvast.server.agent.model.v1.SignatureData;
import se.crisp.codekvast.server.agent.model.v1.UsageData;
import se.crisp.codekvast.server.codekvast_server.exceptions.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;
import javax.validation.Valid;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A HTTP REST Controller that handles requests from the Codekvast Agent.
 * <p/>
 * It validates the POST data and delegates to StorageService.
 *
 * @author Olle Hallin
 */
@RestController
@Slf4j
public class AgentController {

    @NonNull
    private final StorageService storageService;

    @NonNull
    private final Validator validator;

    @Inject
    public AgentController(StorageService storageService, Validator validator) {
        this.storageService = storageService;
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
        log.warn("Application exception: " + e);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_V1_JVM_RUN, method = RequestMethod.POST)
    public void receiveJvmRunDataV1(@Valid @RequestBody JvmRunData data) throws CodekvastException {
        log.info("Received {}", data);
        storageService.storeJvmRunData(data);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_V1_SIGNATURES, method = RequestMethod.POST)
    public void receiveSignatureDataV1(@Valid @RequestBody SignatureData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Received {}", data);
        } else {
            log.debug("Received {} signatures from {}", data.getSignatures().size(), data.getHeader());
        }
        storageService.storeSignatureData(data);
    }

    @RequestMapping(value = AgentRestEndpoints.UPLOAD_V1_USAGE, method = RequestMethod.POST)
    public void receiveUsageDataV1(@Valid @RequestBody UsageData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Received {}", data);
        } else {
            log.debug("Received {} usages from {}", data.getUsage().size(), data.getHeader());
        }
        storageService.storeUsageData(data);
    }

    @RequestMapping(value = AgentRestEndpoints.PING, method = RequestMethod.POST)
    public Pong ping(@Valid @RequestBody Ping data) {
        log.debug("Received {}", data);
        return Pong.builder().message("You said " + data.getMessage()).build();
    }
}
