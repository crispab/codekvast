/*
 * Copyright (c) 2015-2017 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.crisp.codekvast.agent.lib.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import se.crisp.codekvast.agent.lib.config.CodekvastConfig;
import se.crisp.codekvast.agent.lib.model.Invocation;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Low-level file utilities used by the codekvast daemon and the collector.
 *
 * @author olle.hallin@crisp.se
 */
@UtilityClass
@Slf4j
public final class FileUtils {

    private static final String UTF_8 = "UTF-8";

    static final String CONSUMED_SUFFIX = ".consumed";

    public static void deleteAllConsumedInvocationDataFiles(File file) {
        File[] files = file.getParentFile().listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().matches(file.getName() + "(\\.[0-9]+)?" + CONSUMED_SUFFIX + "$")) {
                    f.delete();
                }
            }
        }
    }

    public static void resetAllConsumedInvocationDataFiles(File file) {
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

    public static List<Invocation> consumeAllInvocationDataFiles(File file) {
        List<Invocation> result = new ArrayList<Invocation>();
        File[] files = file.getParentFile().listFiles();
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                if (f.getName().matches(file.getName() + "(\\.[0-9]+)?$")) {
                    result.addAll(readInvocationDataFrom(f));
                    f.renameTo(new File(f.getAbsolutePath() + CONSUMED_SUFFIX));
                }
            }
        }
        return result;
    }

    public static List<Invocation> readInvocationDataFrom(File file) {
        List<Invocation> result = new ArrayList<Invocation>();
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
                    result.add(new Invocation(line, recordingStartedAtMillis));
                }
            }
        } catch (Exception ignore) {
            // ignore all exceptions
        } finally {
            safeClose(in);
        }
        return result;
    }

    public static void writeInvocationDataTo(File file, int publishCount, long recordingStartedAtMillis, Set<String> signatures) {
        if (!signatures.isEmpty()) {
            long startedAt = System.currentTimeMillis();

            File tmpFile = null;
            PrintStream out = null;

            try {
                tmpFile = File.createTempFile("codekvast", ".tmp", file.getParentFile());
                out = new PrintStream(tmpFile, UTF_8);

                Date publishedAt = new Date();
                Date recordedAt = new Date(recordingStartedAtMillis);
                out.printf(Locale.ENGLISH, "# Codekvast recording data #%d at %s, methods invoked since %s%n", publishCount, publishedAt,
                           recordedAt);
                out.println(recordingStartedAtMillis);
                int count = 0;
                for (String sig : signatures) {
                    out.println(SignatureUtils.stripModifiersAndReturnType(sig));
                    count += 1;
                }

                long elapsed = System.currentTimeMillis() - startedAt;
                out.printf(Locale.ENGLISH, "# Publishing #%d at %s took %d ms, number of methods: %d%n", publishCount, publishedAt, elapsed,
                           count);
                out.flush();
            } catch (IOException e) {
                log.error("Codekvast cannot publish invocation data to {}: {}", file, e);
            } finally {
                safeClose(out);
            }

            safeRename(tmpFile, makeUnique(file));
        }
    }

    private static File makeUnique(File file) {
        int count = 0;
        File result;
        File consumed;
        do {
            result = new File(String.format("%s.%05d", file.getAbsolutePath(), count));
            consumed = new File(String.format("%s.%05d%s", file.getAbsolutePath(), count, CONSUMED_SUFFIX));
            count += 1;
        } while (result.exists() || consumed.exists());
        return result;
    }

    private static void safeRename(File from, File to) {
        if (from != null && to != null) {
            renameFile(from, to);
        }
    }

    public static void renameFile(File from, File to) {
        if (!from.renameTo(to)) {
            log.error("Cannot rename {} to {}", from.getAbsolutePath(),
                      to.getAbsolutePath());
            from.delete();
        }
    }

    public static void writePropertiesTo(File file, Object object, String comment) {
        Writer out = null;
        try {
            file.getParentFile().mkdirs();

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
            log.error("Cannot write {}: {}", file, e);
        } catch (IllegalAccessException e) {
            log.error("Cannot write {}: {}", file, e);
        } finally {
            safeClose(out);
        }
    }

    public static void extractFieldValuesFrom(Object object, Set<String> lines) throws IllegalAccessException {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Object value = field.get(object);
                if (value instanceof CodekvastConfig) {
                    extractFieldValuesFrom(value, lines);
                } else if (value != null) {
                    lines.add(String.format("%s = %s", field.getName(),
                                            ConfigUtils.expandVariables(null, value.toString())
                                                       .replace("\\", "\\\\")
                                                       .replace(":", "\\:")));
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
