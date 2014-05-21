package duck.spike.util;

import java.io.*;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Olle Hallin
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class UsageUtils {

    private UsageUtils() {
        // Utility class
    }

    public static Map<String, Usage> readFromFile(File file) {
        Map<String, Usage> result = new TreeMap<String, Usage>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = in.readLine()) != null) {
                Usage usage = Usage.parse(line);
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

    public static void dumpUsageData(File file, int dumpCount, Iterable<Usage> usages) {
        long startedAt = System.currentTimeMillis();

        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());
            PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));

            Date dumpedAt = new Date();
            out.printf(Locale.ENGLISH, "# Duck usage results #%d at %s%n", dumpCount, dumpedAt);
            out.println("# lastUsedMillis:signature");

            int count = 0;
            for (Usage usage : usages) {
                out.println(usage);
                count += 1;
            }

            out.flush();
            out.close();

            renameFile(tmpFile, file);

            long elapsed = System.currentTimeMillis() - startedAt;
            out.printf(Locale.ENGLISH, "# Dump #%d at %s took %d ms, number of methods: %d%n", dumpCount, dumpedAt, elapsed, count);
        } catch (IOException e) {
            System.err.println("DUCK cannot dump usage data to " + file + ": " + e);
        }
    }

    public static void renameFile(File from, File to) {
        if (!from.renameTo(to)) {
            System.err.printf(Locale.ENGLISH, "%s cannot rename %s to %s%n", "DUCK", from.getAbsolutePath(),
                              to.getAbsolutePath());
            from.delete();
        }
    }
}
