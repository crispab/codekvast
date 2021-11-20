package io.codekvast.junit5.extensions; /*
                                         * Copyright 2012-2017 the original author or authors.
                                         *
                                         * Licensed under the Apache License, Version 2.0 (the "License");
                                         * you may not use this file except in compliance with the License.
                                         * You may obtain a copy of the License at
                                         *
                                         *      http://www.apache.org/licenses/LICENSE-2.0
                                         *
                                         * Unless required by applicable law or agreed to in writing, software
                                         * distributed under the License is distributed on an "AS IS" BASIS,
                                         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                                         * See the License for the specific language governing permissions and
                                         * limitations under the License.
                                         */

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.codekvast.junit5.extensions.CaptureSystemOutput.OutputCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Demonstration of how to capture output to {@code System.out} and {@code System.err} via a custom
 * JUnit Jupiter extension.
 *
 * @author Sam Brannen
 */
@CaptureSystemOutput
class CaptureSystemOutputTest {

  @BeforeEach
  void beforeEach(OutputCapture outputCapture) {
    assertFalse(outputCapture.toString().contains("@BeforeEach"));

    System.out.println("@BeforeEach");

    assertTrue(outputCapture.toString().contains("@BeforeEach"));
  }

  @AfterEach
  void afterEach(OutputCapture outputCapture) {
    System.out.println("@AfterEach");

    outputCapture.expect(containsString("@BeforeEach"));
    outputCapture.expect(containsString("@AfterEach"));
  }

  @Test
  void systemOut(OutputCapture outputCapture) {
    outputCapture.expect(containsString("SYS_OUT #1"));

    System.out.println("Printed to SYS_OUT #1");

    // The following would cause the test to fail after the
    // test method completes.
    // outputCapture.expect(containsString("Foo!"));
  }

  @Test
  void systemErr(OutputCapture outputCapture) {
    outputCapture.expect(containsString("SYS_ERR #2"));

    System.err.println("Printed to SYS_ERR #2");
  }

  @Test
  void systemOutAndSystemErr(OutputCapture outputCapture) {
    outputCapture.expect(containsString("SYS_OUT #3"));
    outputCapture.expect(containsString("SYS_ERR #3"));

    System.out.println("Printed to SYS_OUT #3");
    System.err.println("Printed to SYS_ERR #3");
  }
}
