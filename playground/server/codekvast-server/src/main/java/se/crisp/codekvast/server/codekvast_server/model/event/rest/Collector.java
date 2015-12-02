package se.crisp.codekvast.server.codekvast_server.model.event.rest;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * A REST message sent from the web layer when doing something with a collector.
 *
 * @author olle.hallin@crisp.se
 */
@Data
public class Collector {
    @NotNull
    private String appName;

    @NotNull
    private String appVersion;

    @NotNull
    private String hostname;
}
