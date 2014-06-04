package se.crisp.duck.agent.util;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Olle Hallin
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class SensorUtils {

    private static final String CHARSET_NAME = "UTF-8";

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

    public static void writePropertiesTo(File file, Object object, String comment) {
        try {
            Properties props = new Properties();
            for (Field field : object.getClass().getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    props.put(field.getName(), field.get(object).toString());
                }
            }
            writePropertiesToFile(file, props, comment);
        } catch (IOException e) {
            System.err.println("Cannot write " + file + ": " + e);
        } catch (IllegalAccessException e) {
            System.err.println("Cannot write " + file + ": " + e);
        }
    }

    private static void writePropertiesToFile(File file, Properties props, String comment) throws IOException {
        Writer out = new OutputStreamWriter(new FileOutputStream(file), CHARSET_NAME);
        props.store(out, comment);
        out.close();
    }

    public static Properties readPropertiesFrom(File file) throws IOException {
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("'%s' does not exist", file.getAbsolutePath()));
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException(String.format("'%s' is not a file", file.getAbsolutePath()));
        }

        if (!file.canRead()) {
            throw new IllegalArgumentException(String.format("Cannot read '%s'", file.getAbsolutePath()));
        }

        Properties props = new Properties();
        Reader reader = new InputStreamReader(new FileInputStream(file), CHARSET_NAME);
        props.load(reader);
        reader.close();
        return props;
    }
}
