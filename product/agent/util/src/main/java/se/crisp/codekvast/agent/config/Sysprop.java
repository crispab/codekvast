package se.crisp.codekvast.agent.config;

/**
 * Definition of system properties used by the agent or collector.
 *
 * @author Olle Hallin
 */
public enum Sysprop {
    AGENT_CONFIGURATION,
    COLLECTOR_CONFIGURATION;

    @Override
    public String toString() {
        return "codekvast." + name().toLowerCase().replace('_', '.');
    }
}
