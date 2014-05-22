package duck.spike.util;

import java.io.*;
import java.util.*;

/**
 * @author Olle Hallin
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class SensorUtils {

    private SensorUtils() {
        // Utility class
    }

    public static List<Usage> readUsageFrom(File file) {
        List<Usage> result = new ArrayList<Usage>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = in.readLine()) != null) {
                Usage usage = Usage.parse(line);
                if (usage != null) {
                    result.add(usage);
                }
            }
        } catch (Exception ignore) {
            // ignore all exceptions
        }
        return result;
    }

    public static void dumpUsageData(File file, int dumpCount, Map<String, Long> usages) {
        long startedAt = System.currentTimeMillis();

        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());
            PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));

            Date dumpedAt = new Date();
            out.printf(Locale.ENGLISH, "# Duck usage results #%d at %s%n", dumpCount, dumpedAt);
            out.println("# lastUsedMillis:signature");

            int count = 0;
            for (Map.Entry<String, Long> entry : usages.entrySet()) {
                out.println(new Usage(entry.getKey(), entry.getValue()));
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
