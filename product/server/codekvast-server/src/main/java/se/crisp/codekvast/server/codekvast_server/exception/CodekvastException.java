package se.crisp.codekvast.server.codekvast_server.exception;

/**
 * @author olle.hallin@crisp.se
 */
public class CodekvastException extends Exception {

    public CodekvastException(String message) {
        super(message);
    }

    public CodekvastException(String message, Throwable cause) {
        super(message, cause);
    }
}
