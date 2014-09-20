package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
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
@Controller
@Slf4j
public class WebController {
    public static final String TOPIC_SIGNATURES = "/topic/signatures";

    private final StorageService storageService;

    @Inject
    public WebController(StorageService storageService) {
        this.storageService = storageService;
    }

    @MessageMapping("/hello/**")
    @SendTo(TOPIC_SIGNATURES)
    public Collection<UsageDataEntry> hello(Message message) {
        log.debug("Received {}", message);
        return storageService.getSignatures();
    }

}
