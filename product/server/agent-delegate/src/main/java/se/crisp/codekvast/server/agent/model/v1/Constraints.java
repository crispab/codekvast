package se.crisp.codekvast.server.agent.model.v1;

/**
 * Constraints of the REST interface.
 *
 * @author Olle Hallin
 */
public interface Constraints {
    int MAX_APP_NAME_LENGTH = 100;
    int MAX_APP_VERSION_LENGTH = 100;
    int MAX_CUSTOMER_NAME_LENGTH = 100;
    int MAX_ENVIRONMENT_NAME_LENGTH = 100;
    int MIN_FINGERPRINT_LENGTH = 30;
    int MAX_FINGERPRINT_LENGTH = 50;
    int MAX_HOST_NAME_LENGTH = 255;
    int MAX_SIGNATURE_LENGTH = 255;
}
