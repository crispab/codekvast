package duck.spike.util;

import lombok.Value;

import java.io.*;
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

    @Override
    public String toString() {
        return String.format("%14d:%s", usedAtMillis, signature);
    }

    public static Map<String, Usage> readUsagesFromFile(File file) {
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
        } catch (IOException ignore) {
            // ignore
        }
        return result;
    }

    private static Usage parse(String line) {
        Matcher m = CSV_PATTERN.matcher(line);
        return m.matches() ? new Usage(m.group(2), Long.parseLong(m.group(1))) : null;
    }
}
