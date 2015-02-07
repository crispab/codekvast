package se.crisp.codekvast.server.agent_api;

/**
 * @author olle.hallin@crisp.se
 */
public class AgentApiException extends Exception {
    private static final long serialVersionUID = 1L;

    public AgentApiException(String message) {
        super(message);
    }

    public AgentApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
