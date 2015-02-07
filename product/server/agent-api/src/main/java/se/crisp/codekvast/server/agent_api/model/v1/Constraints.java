package se.crisp.codekvast.server.agent_api.model.v1;

/**
 * Constraints of the REST interface.
 *
 * @author olle.hallin@crisp.se
 */
public interface Constraints {
    int MAX_APP_NAME_LENGTH = 100;
    int MAX_APP_VERSION_LENGTH = 100;
    int MAX_CODEKVAST_VCS_ID_LENGTH = 50;
    int MAX_CODEKVAST_VERSION_LENGTH = 20;
    int MAX_COMPUTER_ID_LENGTH = 50;
    int MAX_EMAIL_ADDRESS_LENGTH = 64;
    int MAX_FINGERPRINT_LENGTH = 50;
    int MAX_FULL_NAME_LENGTH = 255;
    int MAX_HOST_NAME_LENGTH = 255;
    int MAX_METHOD_EXECUTION_POINTCUT_LENGTH = 255;
    int MAX_SIGNATURE_LENGTH = 2000;
    int MAX_TAGS_LENGTH = 1000;
    int MAX_USER_NAME_LENGTH = 100;
    int MIN_FINGERPRINT_LENGTH = 30;
}
