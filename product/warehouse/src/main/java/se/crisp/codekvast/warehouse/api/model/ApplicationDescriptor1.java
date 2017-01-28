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
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;

/**
 * Data about the application versions a particular method appears in.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
@EqualsAndHashCode(of = {"name", "version"})
public class ApplicationDescriptor1 implements Comparable<ApplicationDescriptor1> {

    @NonNull
    private final String name;

    @NonNull
    private final String version;

    /**
     * When was this application version first seen?
     */
    @NonNull
    private final Long startedAtMillis;

    /**
     * When was the last time we received data from this application version?
     */
    @NonNull
    private final Long dumpedAtMillis;

    /**
     * When was this particular method invoked in this application version?
     */
    @NonNull
    private final Long invokedAtMillis;

    /**
     * What is the status of this particular method for this application version?
     */
    @NonNull
    private final SignatureStatus status;

    /**
     * Merge two application descriptors, taking the min and max values of both.
     * @param that The other descriptor to merge with.
     * @return An application descriptor with extreme values of startedAtMillis, dumpedAtMillis, invokedAtMillis and the latest status.
     */
    public ApplicationDescriptor1 mergeWith(ApplicationDescriptor1 that) {
        return that == null ? this
                : ApplicationDescriptor1.builder()
                                        .name(this.name)
                                        .version(this.version)
                                        .startedAtMillis(Math.min(this.startedAtMillis, that.startedAtMillis))
                                        .dumpedAtMillis(Math.max(this.dumpedAtMillis, that.dumpedAtMillis))
                                        .invokedAtMillis(Math.max(this.invokedAtMillis, that.invokedAtMillis))
                                        .status(this.invokedAtMillis > that.invokedAtMillis ? this.status : that.status)
                                        .build();
    }

    @Override
    public int compareTo(ApplicationDescriptor1 that) {
        int result = this.name.compareTo(that.name);
        if (result == 0) {
            result = this.version.compareTo(that.version);
        }
        return result;
    }
}
