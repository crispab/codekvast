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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;

import io.codekvast.junit5.extensions.CaptureSystemOutput.OutputCapture;
import java.lang.reflect.Method;
import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit Jupiter extension for capturing output to {@code System.out} and {@code System.err} with
 * expectations supported via Hamcrest matchers.
 *
 * <p>Based on code from Spring Boot's <a
 * href="https://github.com/spring-projects/spring-boot/blob/d3c34ee3d1bfd3db4a98678c524e145ef9bca51c/spring-boot-project/spring-boot-tools/spring-boot-test-support/src/main/java/org/springframework/boot/testsupport/rule/OutputCapture.java">OutputCapture</a>
 * rule for JUnit 4 by Phillip Webb and Andy Wilkinson.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class CaptureSystemOutputExtension
    implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

  @Override
  public void beforeEach(ExtensionContext context) {
    getOutputCapture(context).captureOutput();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    OutputCapture outputCapture = getOutputCapture(context);

    try {
      if (!outputCapture.matchers.isEmpty()) {
        String output = outputCapture.toString();
        assertThat(output, allOf(outputCapture.matchers));
      }
    } finally {
      outputCapture.releaseOutput();
    }
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    boolean isTestMethodLevel = extensionContext.getTestMethod().isPresent();
    boolean isOutputCapture = parameterContext.getParameter().getType() == OutputCapture.class;
    return isTestMethodLevel && isOutputCapture;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return getOutputCapture(extensionContext);
  }

  private OutputCapture getOutputCapture(ExtensionContext context) {
    return getStore(context).getOrComputeIfAbsent(OutputCapture.class);
  }

  private Store getStore(ExtensionContext context) {
    return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
  }
}
