package se.crisp.codekvast.agent.daemon.util;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

import java.net.ConnectException;

import static java.lang.String.format;

/**
 * Utility class for consistent logging.
 */
@UtilityClass
public class LogUtil {

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

    public static String humanReadableByteCount(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        }
        int exponent = (int) (Math.log(bytes) / Math.log(1000));
        String unit = " kMGTPE".charAt(exponent) + "B";
        return format("%.1f %s", bytes / Math.pow(1000, exponent), unit);
    }


    private static Throwable getRootCause(Throwable t) {
        return t.getCause() == null ? t : getRootCause(t.getCause());
    }

}
