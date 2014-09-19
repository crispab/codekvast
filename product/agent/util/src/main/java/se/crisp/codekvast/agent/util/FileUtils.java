package se.crisp.codekvast.agent.util;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.*;

/**
 * Low-level file utlities used by the codekvast agent and sensor.
 *
 * @author Olle Hallin
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class FileUtils {

    private static final String CHARSET_NAME = "UTF-8";

    private FileUtils() {
        // Utility class
    }

    public static List<Usage> readUsageDataFrom(File file) {
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

    public static void writeUsageDataTo(File file, int dumpCount, Map<String, Long> usages) {
        long startedAt = System.currentTimeMillis();

        File tmpFile = null;
        PrintStream out = null;

        try {
            tmpFile = File.createTempFile("codekvast", ".tmp", file.getParentFile());
            out = new PrintStream(tmpFile, CHARSET_NAME);

            Date dumpedAt = new Date();
            out.printf(Locale.ENGLISH, "# CodeKvast usage results #%d at %s%n", dumpCount, dumpedAt);
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
            System.err.println("CodeKvast cannot dump usage data to " + file + ": " + e);
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
            System.err.printf(Locale.ENGLISH, "%s cannot rename %s to %s%n", "CodeKvast", from.getAbsolutePath(),
                              to.getAbsolutePath());
            from.delete();
        }
    }

    public static void writePropertiesTo(File file, Object object, String comment) {
        Writer out = null;
        try {
            // Write the properties alphabetically
            Set<String> lines = new TreeSet<String>();

            for (Field field : object.getClass().getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    lines.add(String.format("%s = %s", field.getName(), field.get(object).toString().replace("\\", "\\\\").replace(":", "\\:")));
                }
            }

            out = new OutputStreamWriter(new FileOutputStream(file), CHARSET_NAME);
            out.write(String.format("# %s%n", comment));
            out.write(String.format("#%n"));
            for (String line : lines) {
                out.write(String.format("%s%n", line));
            }

        } catch (IOException e) {
            System.err.println("Cannot write " + file + ": " + e);
        } catch (IllegalAccessException e) {
            System.err.println("Cannot write " + file + ": " + e);
        } finally {
            safeClose(out);
        }
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

        return readPropertiesFrom(new FileInputStream(file));
    }

    private static Properties readPropertiesFrom(InputStream inputStream) throws IOException {
        Properties result = new Properties();
        Reader reader = null;
        try {
            reader = new InputStreamReader(inputStream, CHARSET_NAME);
            result.load(reader);
        } finally {
            safeClose(reader);
        }
        return result;
    }

    public static Properties readPropertiesFrom(URI uri) throws IOException {
        if (uri.getScheme().equals("classpath")) {
            return readPropertiesFrom(FileUtils.class.getResourceAsStream(uri.getPath()));
        }
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
            throw new RuntimeException("CodeKvast cannot create " + file, e);
        } finally {
            safeClose(writer);
        }
    }

}
