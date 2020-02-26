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
package io.codekvast.dashboard.dashboard.model.methods;

import io.codekvast.javaagent.model.v2.SignatureStatus2;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;

/** @author olle.hallin@crisp.se */
// Cannot use @Value here, since that will prohibit computed fields.
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods"})
@Data
@Setter(value = AccessLevel.NONE)
@Builder(toBuilder = true)
public class MethodDescriptor1 {

  @NonNull private final Long id;

  @NonNull private final String signature;

  /** public, protected, package-private or private */
  @NonNull private final String visibility;

  /** static, final, etc */
  private final String modifiers;

  private final Boolean bridge;

  private final Boolean synthetic;

  private final String packageName;

  private final String declaringType;

  @Singular private final SortedSet<ApplicationDescriptor> occursInApplications;

  @Singular private final SortedSet<EnvironmentDescriptor> collectedInEnvironments;

  // Computed fields, to make it work with Gson. Gson does not serialize using getters.
  private Long collectedSinceMillis;
  private Long collectedToMillis;
  private Long lastInvokedAtMillis;
  private Integer collectedDays;
  private Integer trackedPercent;
  private Set<SignatureStatus2> statuses;
  private Set<String> tags;

  @Singular private SortedSet<String> locations;

  /**
   * Assigns values to all computed fields.
   *
   * @return this
   */
  public MethodDescriptor1 computeFields() {
    this.collectedSinceMillis =
        occursInApplications
            .stream()
            .map(ApplicationDescriptor::getStartedAtMillis)
            .reduce(Math::min)
            .orElse(0L);

    this.collectedToMillis =
        occursInApplications
            .stream()
            .map(ApplicationDescriptor::getPublishedAtMillis)
            .reduce(Math::max)
            .orElse(0L);

    int dayInMillis = 24 * 60 * 60 * 1000;
    this.collectedDays =
        Math.toIntExact((this.collectedToMillis - this.collectedSinceMillis) / dayInMillis);
    this.lastInvokedAtMillis =
        occursInApplications
            .stream()
            .map(ApplicationDescriptor::getInvokedAtMillis)
            .reduce(Math::max)
            .orElse(0L);
    this.statuses =
        occursInApplications
            .stream()
            .map(ApplicationDescriptor::getStatus)
            .collect(Collectors.toSet());
    this.tags = new TreeSet<>();
    collectedInEnvironments.stream().map(EnvironmentDescriptor::getTags).forEach(tags::addAll);
    long tracked =
        occursInApplications
            .stream()
            .map(ApplicationDescriptor::getStatus)
            .filter(SignatureStatus2::isTracked)
            .count();
    this.trackedPercent = (int) Math.round(tracked * 100D / occursInApplications.size());
    return this;
  }
}
