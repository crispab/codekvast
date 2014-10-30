package se.crisp.codekvast.server.agent;

/**
 * @author Olle Hallin
 */
public class ServerDelegateException extends Exception {
    private static final long serialVersionUID = 1L;

    public ServerDelegateException(String message) {
        super(message);
    }

    public ServerDelegateException(String message, Throwable cause) {
        super(message, cause);
    }
}
