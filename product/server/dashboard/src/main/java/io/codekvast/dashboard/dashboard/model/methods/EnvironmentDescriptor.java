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
package io.codekvast.dashboard.dashboard.model.methods;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Setter;

/**
 * Information the environments a particular method appears in.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@Setter(AccessLevel.NONE)
@Builder
@EqualsAndHashCode(of = "name")
public class EnvironmentDescriptor implements Comparable<EnvironmentDescriptor> {

  private static final int ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000;
  /** The name of the environment */
  @NonNull private final String name;

  /** When was collection started in this environment? */
  @NonNull private final Long collectedSinceMillis;

  /** When was the last instant collection data was received from this environment? */
  @NonNull private final Long collectedToMillis;

  /** When was the last instant this particular method was invoked in this environment? */
  @NonNull private final Long invokedAtMillis;

  // Computed fields to make it usable with Gson, which only serializes fields.
  private int collectedDays;

  public EnvironmentDescriptor computeFields() {
    this.collectedDays =
        Math.toIntExact((collectedToMillis - collectedSinceMillis) / ONE_DAY_IN_MILLIS);
    return this;
  }

  /**
   * Merges this environment with another.
   *
   * @param that The environment descriptor to merge with.
   * @return A new object with extreme values of the numerical values and the union of host names
   *     and tags.
   */
  public EnvironmentDescriptor mergeWith(EnvironmentDescriptor that) {
    return that == null
        ? this
        : EnvironmentDescriptor.builder()
            .name(this.name)
            .invokedAtMillis(Math.max(this.invokedAtMillis, that.invokedAtMillis))
            .collectedToMillis(Math.max(this.collectedToMillis, that.collectedToMillis))
            .collectedSinceMillis(Math.min(this.collectedSinceMillis, that.collectedSinceMillis))
            .build()
            .computeFields();
  }

  /**
   * Compares by name.
   *
   * @param that The other environment descriptor
   * @return this.name.compareTo(that.name)
   */
  @Override
  public int compareTo(EnvironmentDescriptor that) {
    return this.name.compareTo(that.name);
  }
}
