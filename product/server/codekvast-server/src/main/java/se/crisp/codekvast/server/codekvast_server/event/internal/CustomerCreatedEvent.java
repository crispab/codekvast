package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Value;
import se.crisp.codekvast.server.codekvast_server.model.Customer;

/**
 * @author Olle Hallin
 */
@Value
public class CustomerCreatedEvent {
    private final Customer customer;
}
