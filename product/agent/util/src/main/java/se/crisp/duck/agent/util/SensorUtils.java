package se.crisp.duck.agent.util;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
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
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), CHARSET_NAME));
            String line;
            while ((line = in.readLine()) != null) {
                Usage usage = Usage.parse(line);
                if (usage != null) {
                    result.add(usage);
                }
            }
        } catch (Exception ignore) {
            // ignore all exceptions
        } finally {
            safeClose(in);
        }
        return result;
    }

    public static void dumpUsageData(File file, int dumpCount, Map<String, Long> usages) {
        long startedAt = System.currentTimeMillis();

        File tmpFile = null;
        PrintStream out = null;

        try {
            tmpFile = File.createTempFile("duck", ".tmp", file.getParentFile());
            out = new PrintStream(tmpFile, CHARSET_NAME);

            Date dumpedAt = new Date();
            out.printf(Locale.ENGLISH, "# Duck usage results #%d at %s%n", dumpCount, dumpedAt);
            out.println("# lastUsedMillis:signature");

            int count = 0;
            for (Map.Entry<String, Long> entry : usages.entrySet()) {
                out.println(new Usage(entry.getKey(), entry.getValue()));
                count += 1;
            }

            long elapsed = System.currentTimeMillis() - startedAt;
            out.printf(Locale.ENGLISH, "# Dump #%d at %s took %d ms, number of methods: %d%n", dumpCount, dumpedAt, elapsed, count);
            out.flush();
        } catch (IOException e) {
            System.err.println("DUCK cannot dump usage data to " + file + ": " + e);
        } finally {
            safeClose(out);
        }

        safeRename(tmpFile, file);
    }

    private static void safeRename(File from, File to) {
        if (from != null && to != null) {
            renameFile(from, to);
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
        safeClose(out);
    }

    public static Properties readPropertiesFrom(String path) throws IOException {
        return readPropertiesFrom(new File(path));
    }

    public static Properties readPropertiesFrom(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException(String.format("'%s' does not exist", file.getAbsolutePath()));
        }

        if (!file.isFile()) {
            throw new IOException(String.format("'%s' is not a file", file.getAbsolutePath()));
        }

        if (!file.canRead()) {
            throw new IOException(String.format("Cannot read '%s'", file.getAbsolutePath()));
        }

        Properties props = new Properties();
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), CHARSET_NAME);
            props.load(reader);
        } finally {
            safeClose(reader);
        }
        return props;
    }

    public static Properties readPropertiesFrom(URI uri) throws IOException {
        return readPropertiesFrom(new File(uri));
    }

    private static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    public static void safeDelete(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public static void writeToFile(String text, File file) {
        Writer writer = null;
        try {
            file.getParentFile().mkdirs();
            writer = new OutputStreamWriter(new FileOutputStream(file), CHARSET_NAME);
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("DUCK cannot create " + file, e);
        } finally {
            safeClose(writer);
        }
    }

}
