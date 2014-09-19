package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.codekvast_server.event.CodekvastEvent;

/**
 * @author Olle Hallin
 */
@Slf4j
@Component
public class ClientNotifierImpl implements ApplicationListener<CodekvastEvent> {

    @Override
    public void onApplicationEvent(CodekvastEvent event) {
        log.debug("Handling {}", event);

        // TODO: notify clients
    }
}
