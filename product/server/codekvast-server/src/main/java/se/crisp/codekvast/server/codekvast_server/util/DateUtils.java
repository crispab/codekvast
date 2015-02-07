package se.crisp.codekvast.server.codekvast_server.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Thread-safe utility class for handling dates.
 *
 * @author olle.hallin@crisp.se
 */
public class DateUtils {

    private static final ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    private DateUtils() {
        // utility class
    }

    public static String formatDate(Date date) {
        return sdf.get().format(date);
    }

    public static String formatDate(long timestampMillis) {
        return timestampMillis == 0L ? "" : sdf.get().format(new Date(timestampMillis));
    }

    public static String getAgeXX(long now, long timestampMillis) {
        if (timestampMillis == 0L) {
            return "";
        }

        long age = now - timestampMillis;

        long minutes = 60 * 1000L;
        if (age < 60 * minutes) {
            return String.format("%d min", age / minutes);
        }

        long hours = minutes * 60;
        if (age < 24 * hours) {
            return String.format("%d hours", age / hours);
        }
        long days = hours * 24;
        if (age < 30 * days) {
            return String.format("%d days", age / days);
        }

        long week = days * 7;
        return String.format("%d weeks", age / week);
    }


}
