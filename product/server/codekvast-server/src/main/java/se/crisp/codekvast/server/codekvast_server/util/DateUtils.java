package se.crisp.codekvast.server.codekvast_server.util;

import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Thread-safe utility class for handling dates.
 *
 * @author olle.hallin@crisp.se
 */
@UtilityClass
public class DateUtils {

    private static final ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    public static String formatDate(Date date) {
        return sdf.get().format(date);
    }

    public static String formatDate(long timestampMillis) {
        return timestampMillis == 0L ? "" : sdf.get().format(new Date(timestampMillis));
    }

}
