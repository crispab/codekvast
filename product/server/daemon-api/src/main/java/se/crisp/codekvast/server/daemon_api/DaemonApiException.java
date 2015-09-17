package se.crisp.codekvast.server.daemon_api;

/**
 * @author olle.hallin@crisp.se
 */
public class DaemonApiException extends Exception {
    private static final long serialVersionUID = 1L;

    public DaemonApiException(String message) {
        super(message);
    }

    public DaemonApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
