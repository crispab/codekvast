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

    @Override
    public String toString() {
        // Make it pretty in JSON...
        return name + " " + version;
    }

    public static ApplicationId of(String name, String version) {
        return new ApplicationId(name, version);
    }
}
