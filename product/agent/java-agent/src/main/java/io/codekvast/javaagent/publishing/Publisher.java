/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
package io.codekvast.javaagent.publishing;

/** @author olle.hallin@crisp.se */
@SuppressWarnings("MethodReturnAlwaysConstant")
public interface Publisher {
  /**
   * What is the nick-name of this publisher implementation.
   *
   * @return The name of the publisher.
   */
  String getName();

  /**
   * Configure this publisher.
   *
   * @param customerId The customerId to use when publishing stuff
   * @param keyValuePairs The specialized config received from the server, a semi-colon separated
   *     list of key=value pairs.
   */
  void configure(long customerId, String keyValuePairs);

  /**
   * How many times has a publication actually been executed?
   *
   * @return The number of performed publications.
   */
  int getSequenceNumber();

  /**
   * Is the publisher enabled?
   *
   * @return true iff the publisher is enabled.
   */
  boolean isEnabled();
}
