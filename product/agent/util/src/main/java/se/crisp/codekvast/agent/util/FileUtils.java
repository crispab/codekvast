package se.crisp.codekvast.agent.util;

import se.crisp.codekvast.agent.config.CodekvastConfig;
import se.crisp.codekvast.agent.model.Usage;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Low-level file utilities used by the codekvast agent and collector.
 *
 * @author Olle Hallin
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class FileUtils {

    private static final String UTF_8 = "UTF-8";

    public static final String CONSUMED_SUFFIX = ".consumed";

    private FileUtils() {
        // Utility class
    }

    public static void deleteAllConsumedUsageDataFiles(File file) {
        File[] files = file.getParentFile().listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().matches(file.getName() + "(\\.[0-9]+)?" + CONSUMED_SUFFIX + "$")) {
                    f.delete();
                }
            }
        }
    }

    public static void resetAllConsumedUsageDataFiles(File file) {
        File[] files = file.getParentFile().listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().matches(file.getName() + "(\\.[0-9]+)?" + CONSUMED_SUFFIX + "$")) {
                    String name = f.getAbsolutePath();
                    String unconsumedName = name.substring(0, name.length() - CONSUMED_SUFFIX.length());
                    f.renameTo(new File(unconsumedName));
                }
            }
        }
    }

    public static List<Usage> consumeAllUsageDataFiles(File file) {
        List<Usage> result = new ArrayList<Usage>();
        File[] files = file.getParentFile().listFiles();
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                if (f.getName().matches(file.getName() + "(\\.[0-9]+)?$")) {
                    result.addAll(readUsageDataFrom(f));
                    f.renameTo(new File(f.getAbsolutePath() + CONSUMED_SUFFIX));
                }
            }
        }
        return result;
    }

    public static List<Usage> readUsageDataFrom(File file) {
        List<Usage> result = new ArrayList<Usage>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF_8));
            String line;
            long recordingStartedAtMillis = -1;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                if (recordingStartedAtMillis == -1) {
                    recordingStartedAtMillis = Long.parseLong(line);
                } else {
                    result.add(new Usage(line, recordingStartedAtMillis));
                }
            }
        } catch (Exception ignore) {
            // ignore all exceptions
        } finally {
            safeClose(in);
        }
        return result;
    }

    public static void writeUsageDataTo(File file, int dumpCount, long recordingStartedAtMillis, Set<String> signatures) {
        if (!signatures.isEmpty()) {
            long startedAt = System.currentTimeMillis();

            File tmpFile = null;
            PrintStream out = null;

            try {
                tmpFile = File.createTempFile("codekvast", ".tmp", file.getParentFile());
                out = new PrintStream(tmpFile, UTF_8);

                Date dumpedAt = new Date();
                Date recordedAt = new Date(recordingStartedAtMillis);
                out.printf(Locale.ENGLISH, "# Codekvast usage results #%d at %s, methods used since %s%n", dumpCount, dumpedAt, recordedAt);
                out.println(recordingStartedAtMillis);
                int count = 0;
                for (String sig : signatures) {
                    out.println(sig);
                    count += 1;
                }

                long elapsed = System.currentTimeMillis() - startedAt;
                out.printf(Locale.ENGLISH, "# Dump #%d at %s took %d ms, number of methods: %d%n", dumpCount, dumpedAt, elapsed, count);
                out.flush();
            } catch (IOException e) {
                System.err.println("Codekvast cannot dump usage data to " + file + ": " + e);
            } finally {
                safeClose(out);
            }

            safeRename(tmpFile, makeUnique(file));
        }
    }

    private static File makeUnique(File file) {
        int count = 0;
        File result = new File(file.getAbsolutePath());
        File consumed = new File(file.getAbsolutePath() + CONSUMED_SUFFIX);
        while (result.exists() || consumed.exists()) {
            count += 1;
            result = new File(file.getAbsolutePath() + "." + count);
            consumed = new File(file.getAbsolutePath() + "." + count + CONSUMED_SUFFIX);
        }
        return result;
    }

    private static void safeRename(File from, File to) {
        if (from != null && to != null) {
            renameFile(from, to);
        }
    }

    public static void renameFile(File from, File to) {
        if (!from.renameTo(to)) {
            System.err.printf(Locale.ENGLISH, "%s cannot rename %s to %s%n", "Codekvast", from.getAbsolutePath(),
                              to.getAbsolutePath());
            from.delete();
        }
    }

    public static void writePropertiesTo(File file, Object object, String comment) {
        Writer out = null;
        try {
            // Write the properties alphabetically
            Set<String> lines = new TreeSet<String>();

            extractFieldValuesFrom(object, lines);

            out = new OutputStreamWriter(new FileOutputStream(file), UTF_8);
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

    protected static void extractFieldValuesFrom(Object object, Set<String> lines) throws IllegalAccessException {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Object value = field.get(object);
                if (value instanceof CodekvastConfig) {
                    extractFieldValuesFrom(value, lines);
                } else if (value != null) {
                    lines.add(String.format("%s = %s", field.getName(),
                                            value.toString().replace("\\", "\\\\").replace(":", "\\:")));
                } else {
                    lines.add(String.format("# %s = ", field.getName()));
                }
            }
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
            reader = new InputStreamReader(inputStream, UTF_8);
            result.load(reader);
        } finally {
            safeClose(reader);
        }
        return result;
    }

    public static Properties readPropertiesFrom(URI uri) throws IOException, URISyntaxException {
        String scheme = uri.getScheme();
        if (scheme == null) {
            return readPropertiesFrom(new File(new URI("file:" + uri.getPath())));
        }
        if (scheme.equals("classpath")) {
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
            writer = new OutputStreamWriter(new FileOutputStream(file), UTF_8);
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Codekvast cannot create " + file, e);
        } finally {
            safeClose(writer);
        }
    }

}
