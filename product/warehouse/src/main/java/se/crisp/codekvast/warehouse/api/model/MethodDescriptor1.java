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
package se.crisp.codekvast.warehouse.api.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder(toBuilder = true)
public class MethodDescriptor1 {
    @NonNull
    private final Long id;

    @NonNull
    private final String signature;

    /**
     * public, protected, package-private or private
     */
    @NonNull
    private final String visibility;

    /**
     * static, final, etc
     */
    private final String modifiers;

    private final String packageName;

    private final String declaringType;

    @Singular
    private final SortedSet<ApplicationDescriptor1> occursInApplications;

    @Singular
    private final SortedSet<EnvironmentDescriptor1> collectedInEnvironments;

    /**
     * Calculates in how many apps this method is tracked.
     */

    public int getTrackedPercent() {
        long tracked = occursInApplications.stream().map(ApplicationDescriptor1::getStatus).filter(SignatureStatus::isTracked).count();
        return (int) Math.round (tracked * 100D / occursInApplications.size());
    }

    /**
     * Collect all different statuses this method has in all collected apps.
     */
    public Set<SignatureStatus> getStatuses() {
        return occursInApplications.stream().map(ApplicationDescriptor1::getStatus).collect(Collectors.toSet());
    }

    /**
     * Maximum value of occursInApplications.invokedAtMillis;
     */
    public Long getLastInvokedAtMillis() {
        return occursInApplications.stream().map(ApplicationDescriptor1::getInvokedAtMillis).reduce(Math::max).orElse(0L);
    }

    /**
     * Minimum value of occursInApplications.startedAtMillis
     */
    public Long getCollectedSinceMillis() {
        return occursInApplications.stream().map(ApplicationDescriptor1::getStartedAtMillis).reduce(Math::min).orElse(0L);
    }

    /**
     * Maximum value of occursInApplications.getDumpedAtMillis
     */
    public Long getCollectedToMillis() {
        return occursInApplications.stream().map(ApplicationDescriptor1::getDumpedAtMillis).reduce(Math::max).orElse(0L);
    }

    /**
     * Convenience: the difference between {@link #getCollectedToMillis()} and {@link #getCollectedSinceMillis()} expressed as days.
     */
    @SuppressWarnings("unused")
    public int getCollectedDays() {
        int dayInMillis = 24 * 60 * 60 * 1000;
        return Math.toIntExact((getCollectedToMillis() - getCollectedSinceMillis()) / dayInMillis);
    }

    /**
     * Convenience: collects tags from all environments
     */
    @SuppressWarnings("unused")
    public Set<String> getTags() {
        Set<String> result = new TreeSet<>();
        collectedInEnvironments.stream().map(EnvironmentDescriptor1::getTags).forEach(tags -> result.addAll(tags));
        return result;
    }
}
