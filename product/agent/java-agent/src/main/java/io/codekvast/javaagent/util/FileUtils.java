/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.javaagent.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 * Low-level file utilities used by the codekvast agent.
 *
 * @author olle.hallin@crisp.se
 */
@UtilityClass
@Log
public final class FileUtils {

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

    return readPropertiesFrom(new BufferedInputStream(new FileInputStream(file)));
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
        logger.fine("Creating " + parentDir);
        parentDir.mkdirs();
        if (!parentDir.isDirectory()) {
          logger.warning("Failed to create " + parentDir);
        }
      }
      writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)), UTF_8);
      writer.write(text);
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException("Codekvast cannot create " + file, e);
    } finally {
      safeClose(writer);
    }
  }

  @SuppressWarnings("SameParameterValue")
  public static File serializeToFile(Object object, String prefix, String suffix)
      throws IOException {
    long startedAt = System.currentTimeMillis();
    File file = File.createTempFile(prefix, suffix);
    try (ObjectOutputStream oos =
        new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
      oos.writeObject(object);
    }
    logger.fine(
        String.format(
            "Serialized %s in %d ms",
            object.getClass().getSimpleName(), System.currentTimeMillis() - startedAt));
    return file;
  }

  public static <T> T deserializeFromFile(File file, Class<T> classOfT) {
    try (ObjectInputStream ois =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
      return classOfT.cast(ois.readObject());
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
