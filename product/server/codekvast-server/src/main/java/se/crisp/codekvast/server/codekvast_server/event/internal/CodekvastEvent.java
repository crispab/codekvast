package se.crisp.codekvast.server.codekvast_server.event.internal;

import org.springframework.context.ApplicationEvent;

/**
 * @author Olle Hallin
 */
public abstract class CodekvastEvent extends ApplicationEvent {

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the component that published the event (never {@code null})
     */
    public CodekvastEvent(Object source) {
        super(source);
    }
}
