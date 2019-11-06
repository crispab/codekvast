package io.codekvast.backoffice.event;

import io.codekvast.common.messaging.model.CodekvastEvent;
import org.springframework.stereotype.Component;

/**
 * Dispatcher for CodekvastEvents
 *
 * @author olle.hallin@crisp.se
 */
public interface EventHandler {

    /**
     * Handle an event
     *
     * @param event The event to handle. Is never null.
     */
    void handle(CodekvastEvent event);
}
