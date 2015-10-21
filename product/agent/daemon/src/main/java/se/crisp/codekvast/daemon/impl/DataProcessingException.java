package se.crisp.codekvast.daemon.impl;

/**
 * @author olle.hallin@crisp.se
 */
public class DataProcessingException extends RuntimeException {
    public DataProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
