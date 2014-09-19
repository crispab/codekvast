package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;
import java.util.Collection;

/**
 * A HTTP REST Controller that handles requests from the CodeKvast web UI.
 * <p/>
 *
 * @author Olle Hallin
 */
@RestController
@Slf4j
public class UserController {

    private final StorageService storageService;

    @Inject
    public UserController(StorageService storageService, Validator validator) {
        this.storageService = storageService;
    }

    @RequestMapping(value = "/user/signatures/", method = RequestMethod.GET)
    public Collection<UsageDataEntry> getSignatures() {
        log.debug("Retrieving signatures");
        return storageService.getSignatures();
    }

}
