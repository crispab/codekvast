package se.crisp.codekvast.agent.daemon.worker;

/**
 * An exception that signals a file upload failure.
 *
 * @author olle.hallin@crisp.se
 */
public class FileUploadException extends Exception {

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
