package se.crisp.codekvast.agent.util;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

import java.net.ConnectException;

/**
 * Utility class for consistent logging
 */
@UtilityClass
public class LoggingUtil {

    public static void logException(Logger logger, String msg, Exception e) {
        Throwable rootCause = getRootCause(e);
        if (logger.isDebugEnabled() && !(rootCause instanceof ConnectException)) {
            // log with full stack trace
            logger.error(msg, e);
        } else {
            // log a one-liner with the root cause
            logger.error("{}: {}", msg, rootCause.toString());
        }
    }

    private static Throwable getRootCause(Throwable t) {
        return t.getCause() == null ? t : getRootCause(t.getCause());
    }


}
