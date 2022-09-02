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
package io.codekvast.common.messaging;

import java.util.UUID;
import lombok.NonNull;
import org.slf4j.MDC;

/**
 * A wrapper for a ThreadLocal that holds a correlationId.
 *
 * @author olle.hallin@crisp.se
 */
public class CorrelationIdHolder {
  private static final ThreadLocal<String> holder = new ThreadLocal<>();
  private static final String CORRELATION_ID = "correlationId";

  /**
   * Retrieves the thread's correlationId. Creates one if missing.
   *
   * @return The correlationId for the thread. Does never return null.
   */
  public static String get() {
    String id = holder.get();
    if (id == null) {
      id = generateNew();
      set(id);
    }
    return id;
  }

  /**
   * Sets a new correlationId in a ThreadLocal.
   *
   * <p>The value can be obtained e.g., via an HTTP request parameter, or an event received from
   * another service. It also invokes {@code MDC.put("correlationId", id)}.
   *
   * @param id The new id to set. May not be null.
   */
  public static void set(@NonNull String id) {
    holder.set(id);
    MDC.put(CORRELATION_ID, id);
  }

  /**
   * Generates and stores a new random correlationId.
   *
   * <p>It invokes {@link #generateNew()} and {@link #set(String)}
   *
   * @return The new unique random correlationId. Does never return null.
   */
  public static String generateAndSetNew() {
    String id = generateNew();
    set(id);
    return id;
  }

  /**
   * Generates a new random correlationId, but does <b>NOT</b> invoke {@link #set(String)}.
   *
   * @return The new unique random correlationId. Does never return null.
   */
  public static String generateNew() {
    return UUID.randomUUID().toString();
  }

  /**
   * Removes the correlation id from the thread, to prevent leaking to other tasks. It also invokes
   * {@code MDC.remove("correlationId")}.
   *
   * <p>Should be used in a finally block.
   */
  public static void clear() {
    holder.remove();
    MDC.remove(CORRELATION_ID);
  }
}
