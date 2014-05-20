package duck.spike.util;

import lombok.Value;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Olle Hallin
 */
@Value
public class Usage {
    private final static Pattern CSV_PATTERN = Pattern.compile("^\\s*([\\d]+):(.*)");

    private final String signature;
    private final long usedAtMillis;

    public Usage(String signature, long usedAtMillis) {
        this.signature = signature;
        this.usedAtMillis = usedAtMillis;
    }

    /**
     * Formats for CSV output. Will be recognized by {@link #parse(String)}
     *
     * @return usedAtMillis ':' signature
     */
    @Override
    public String toString() {
        return String.format("%14d:%s", usedAtMillis, signature);
    }

    public static Map<String, Usage> readFromFile(File file) {
        Map<String, Usage> result = new HashMap<String, Usage>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = in.readLine()) != null) {
                Usage usage = parse(line);
                if (usage != null) {
                    // Replace whatever usage was there
                    result.put(usage.getSignature(), usage);
                }
            }
        } catch (Exception ignore) {
            // ignore all exceptions
        }
        return result;
    }

    static Usage parse(String line) {
        Matcher m = line == null ? null : CSV_PATTERN.matcher(line);
        return m != null && m.matches() ? new Usage(m.group(2), Long.parseLong(m.group(1))) : null;
    }
}
