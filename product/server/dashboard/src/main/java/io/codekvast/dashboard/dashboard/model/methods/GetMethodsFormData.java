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
package io.codekvast.dashboard.dashboard.model.methods;

import java.util.SortedSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/** @author olle.hallin@crisp.se */
@Value
@Builder
public class GetMethodsFormData {

  @NonNull @Singular private final SortedSet<String> applications;

  @NonNull @Singular private final SortedSet<String> environments;

  @NonNull @Singular private final SortedSet<String> locations;

  private final Integer retentionPeriodDays;

  public static GetMethodsFormData sample() {
    return GetMethodsFormData.builder()
        .application("app2")
        .application("app1")
        .environment("env2")
        .environment("env1")
        .location("shared-lib.jar")
        .location("BOOT-INF/classes/")
        .retentionPeriodDays(14)
        .build();
  }
}
