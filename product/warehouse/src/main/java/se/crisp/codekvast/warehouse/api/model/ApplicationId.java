package se.crisp.codekvast.warehouse.api.model;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationId implements Comparable<ApplicationId> {
    private final String name;
    private final String version;

    @Override
    public int compareTo(ApplicationId that) {
        return this.toString().compareTo(that.toString());
    }

    public static ApplicationId of(ApplicationDescriptor applicationDescriptor) {
        return new ApplicationId(applicationDescriptor.getName(), applicationDescriptor.getVersion());
    }
}
