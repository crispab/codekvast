package se.crisp.codekvast.daemon;

/**
 * @author olle.hallin@crisp.se
 */
public class DataExportException extends Exception {

    public DataExportException(String message) {
        super(message);
    }

    public DataExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
