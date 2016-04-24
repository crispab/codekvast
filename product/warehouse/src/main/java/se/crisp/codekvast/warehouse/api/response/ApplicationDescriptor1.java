package se.crisp.codekvast.warehouse.api.response;

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
