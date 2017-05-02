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
package io.codekvast.javaagent.util;

import io.codekvast.javaagent.config.CodekvastConfig;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Low-level file utilities used by the codekvast daemon and the collector.
 *
 * @author olle.hallin@crisp.se
 */
@UtilityClass
@Slf4j
public final class FileUtils {

    private static final String UTF_8 = "UTF-8";

    public static void writePropertiesTo(File file, Object object, String comment) {
        Writer out = null;
        try {
            file.getParentFile().mkdirs();

            // Write the properties alphabetically
            Set<String> lines = new TreeSet<>();

            extractFieldValuesFrom(object, lines);

            out = new OutputStreamWriter(new FileOutputStream(file), UTF_8);
            out.write(String.format("# %s%n", comment));
            out.write(String.format("#%n"));
            for (String line : lines) {
                out.write(String.format("%s%n", line));
            }

        } catch (IOException | IllegalAccessException e) {
            log.error("Cannot write {}: {}", file, e);
        } finally {
            safeClose(out);
        }
    }

    private static void extractFieldValuesFrom(Object object, Set<String> lines) throws IllegalAccessException {
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

    private static Properties readPropertiesFrom(File file) throws IOException {
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
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.isDirectory()) {
                log.debug("Creating {}", parentDir);
                parentDir.mkdirs();
                if (!parentDir.isDirectory()) {
                    log.warn("Failed to create {}", parentDir);
                }
            }
            writer = new OutputStreamWriter(new FileOutputStream(file), UTF_8);
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Codekvast cannot create " + file, e);
        } finally {
            safeClose(writer);
        }
    }

    static File expandPlaceholders(File file) {
        if (file == null) {
            return null;
        }

        String name = file.getName().replace("#hostname#", Constants.HOST_NAME).replace("#timestamp#", getTimestamp());

        File parentFile = file.getParentFile();
        return parentFile == null ? new File(name) : new File(parentFile, name);
    }

    private static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
    }

    @SuppressWarnings("SameParameterValue")
    public static File serializeToFile(Object object, String prefix, String suffix) throws IOException {
        long startedAt = System.currentTimeMillis();
        File file = File.createTempFile(prefix, suffix);
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            oos.writeObject(object);
        }
        log.debug("Serialized {} in {} ms", object.getClass().getSimpleName(), System.currentTimeMillis() - startedAt);
        return file;
    }

}
