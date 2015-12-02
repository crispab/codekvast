package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.DaemonService;
import se.crisp.codekvast.server.daemon_api.DaemonRestEndpoints;
import se.crisp.codekvast.server.daemon_api.model.test.Ping;
import se.crisp.codekvast.server.daemon_api.model.test.Pong;
import se.crisp.codekvast.server.daemon_api.model.v1.JvmData;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureData;

import javax.inject.Inject;
import javax.validation.Valid;
import java.security.Principal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A HTTP REST Controller that handles requests from the Codekvast Daemon.
 *
 * It validates the POST data and delegates to DaemonService.
 *
 * @author olle.hallin@crisp.se
 */
@RestController
@Slf4j
public class DaemonController {

    @NonNull
    private final DaemonService daemonService;

    @NonNull
    private final Validator validator;

    @Inject
    public DaemonController(DaemonService daemonService, Validator validator) {
        this.daemonService = daemonService;
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

    @RequestMapping(value = DaemonRestEndpoints.UPLOAD_V1_JVM_DATA, method = RequestMethod.POST)
    public void receiveJvmDataV1(@Valid @RequestBody JvmData data, Principal principal) throws CodekvastException {
        long startedAt = System.currentTimeMillis();
        log.debug("Received {} from {}", data, principal.getName());

        daemonService.storeJvmData(principal.getName(), data);

        log.info("Stored JVM info from {} {} in {} ms", data.getAppName(), data.getAppVersion(), System.currentTimeMillis() - startedAt);
    }

    @RequestMapping(value = DaemonRestEndpoints.UPLOAD_V1_SIGNATURES, method = RequestMethod.POST)
    public void receiveSignatureDataV1(@Valid @RequestBody SignatureData data, Principal principal) throws CodekvastException {
        long startedAt = System.currentTimeMillis();
        log.debug("Received {} signatures from {}", data.getSignatures().size(), principal.getName());

        daemonService.storeSignatureData(data);

        log.info("Stored {} signatures in {} ms", data.getSignatures().size(), System.currentTimeMillis() - startedAt);
    }

    @RequestMapping(value = DaemonRestEndpoints.PING, method = RequestMethod.POST)
    public Pong ping(@Valid @RequestBody Ping data, Principal principal) {
        log.info("Received {} from {}", data, principal.getName());
        return Pong.builder().message("You said " + data.getMessage()).build();
    }
}
