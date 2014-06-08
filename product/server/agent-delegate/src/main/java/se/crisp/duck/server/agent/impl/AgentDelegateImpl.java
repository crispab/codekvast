package se.crisp.duck.server.agent.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import se.crisp.duck.server.agent.AgentDelegate;

import java.net.URI;
import java.util.Collection;

/**
 * @author Olle Hallin
 */
@RequiredArgsConstructor
@Slf4j
public class AgentDelegateImpl implements AgentDelegate {

    private final URI serverUri;

    @Override
    public void uploadSignatures(Collection<String> signatures) {
        log.info("Uploading {} signatures to {}", signatures.size(), serverUri);
    }
}
