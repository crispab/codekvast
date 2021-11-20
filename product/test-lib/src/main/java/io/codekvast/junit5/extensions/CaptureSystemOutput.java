/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
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
package io.codekvast.junit5.extensions;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @CaptureSystemOutput} is a JUnit Jupiter extension for capturing output to {@code
 * System.out} and {@code System.err} with expectations supported via Hamcrest matchers.
 *
 * <p>Example Usage
 *
 * <pre style="code">
 * {@literal @}Test
 * {@literal @}CaptureSystemOutput
 * void systemOut(OutputCapture outputCapture) {
 *     outputCapture.expect(containsString("System.out!"));
 *
 *     System.out.println("Printed to System.out!");
 * }
 *
 * {@literal @}Test
 * {@literal @}CaptureSystemOutput
 * void systemErr(OutputCapture outputCapture) {
 *     outputCapture.expect(containsString("System.err!"));
 *
 *     System.err.println("Printed to System.err!");
 * }
 * </pre>
 *
 * <p>Based on code from Spring Boot's <a
 * href="https://github.com/spring-projects/spring-boot/blob/d3c34ee3d1bfd3db4a98678c524e145ef9bca51c/spring-boot-project/spring-boot-tools/spring-boot-test-support/src/main/java/org/springframework/boot/testsupport/rule/OutputCapture.java">OutputCapture</a>
 * rule for JUnit 4 by Phillip Webb and Andy Wilkinson.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
@Documented
@ExtendWith(CaptureSystemOutputExtension.class)
public @interface CaptureSystemOutput {

  /**
   * {@code OutputCapture} captures output to {@code System.out} and {@code System.err}.
   *
   * <p>To obtain an instance of {@code OutputCapture}, declare a parameter of type {@code
   * OutputCapture} in a JUnit Jupiter {@code @Test}, {@code @BeforeEach}, or {@code @AfterEach}
   * method.
   *
   * <p>{@linkplain #expect Expectations} are supported via Hamcrest matchers.
   *
   * <p>To obtain all output to {@code System.out} and {@code System.err}, simply invoke {@link
   * #toString()}.
   *
   * @author Phillip Webb
   * @author Andy Wilkinson
   * @author Sam Brannen
   */
  static class OutputCapture {

    final List<Matcher<? super String>> matchers = new ArrayList<>();

    private CaptureOutputStream captureOut;

    private CaptureOutputStream captureErr;

    private ByteArrayOutputStream copy;

    void captureOutput() {
      this.copy = new ByteArrayOutputStream();
      this.captureOut = new CaptureOutputStream(System.out, this.copy);
      this.captureErr = new CaptureOutputStream(System.err, this.copy);
      System.setOut(new PrintStream(this.captureOut));
      System.setErr(new PrintStream(this.captureErr));
    }

    void releaseOutput() {
      System.setOut(this.captureOut.getOriginal());
      System.setErr(this.captureErr.getOriginal());
      this.copy = null;
    }

    public void flush() {
      try {
        this.captureOut.flush();
        this.captureErr.flush();
      } catch (IOException ex) {
        // ignore
      }
    }

    /**
     * Verify that the captured output is matched by the supplied {@code matcher}.
     *
     * <p>Verification is performed after the test method has executed.
     *
     * @param matcher the matcher
     */
    public void expect(Matcher<? super String> matcher) {
      this.matchers.add(matcher);
    }

    /**
     * Return all captured output to {@code System.out} and {@code System.err} as a single string.
     */
    @Override
    public String toString() {
      flush();
      return this.copy.toString();
    }

    private static class CaptureOutputStream extends OutputStream {

      private final PrintStream original;

      private final OutputStream copy;

      CaptureOutputStream(PrintStream original, OutputStream copy) {
        this.original = original;
        this.copy = copy;
      }

      PrintStream getOriginal() {
        return this.original;
      }

      @Override
      public void write(int b) throws IOException {
        this.copy.write(b);
        this.original.write(b);
        this.original.flush();
      }

      @Override
      public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        this.copy.write(b, off, len);
        this.original.write(b, off, len);
      }

      @Override
      public void flush() throws IOException {
        this.copy.flush();
        this.original.flush();
      }
    }
  }
}
