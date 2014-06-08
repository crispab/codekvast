package se.crisp.duck.server.agent;

import java.util.Collection;

/**
 * @author Olle Hallin
 */
public interface AgentDelegate {
    void uploadSignatures(Collection<String> signatures);
}
