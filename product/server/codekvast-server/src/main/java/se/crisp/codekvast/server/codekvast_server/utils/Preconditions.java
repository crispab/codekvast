package se.crisp.codekvast.server.codekvast_server.utils;

/**
 * @author Olle Hallin
 */
public class Preconditions {

    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
