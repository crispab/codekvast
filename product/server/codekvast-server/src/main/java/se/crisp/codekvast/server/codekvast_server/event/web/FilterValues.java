package se.crisp.codekvast.server.codekvast_server.event.web;

import lombok.Value;
import lombok.experimental.Builder;

import java.util.Collection;

/**
 * A value object containing everything that can be specified as filters in the web layer. It is sent as a STOMP message from the server to
 * the web layer as soon as there is a change in filter values.
 *
 * @author Olle Hallin
 */
@Value
@Builder
public class FilterValues {
    private Collection<String> customerNames;
    private Collection<String> applications;
    private Collection<String> versions;
    private Collection<String> packages;
    private Collection<String> tags;
}
