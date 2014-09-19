package se.crisp.codekvast.server.codekvast_server.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Olle Hallin
 */
public class DateTimeUtils {

    private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS");
        }
    };

    public static String formatDate(long timeMillis) {
        return dateFormat.get().format(new Date(timeMillis));
    }


}
