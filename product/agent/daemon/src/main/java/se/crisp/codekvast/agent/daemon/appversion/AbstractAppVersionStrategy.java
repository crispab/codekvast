package se.crisp.codekvast.agent.daemon.appversion;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author olle.hallin@crisp.se
 */
abstract class AbstractAppVersionStrategy implements AppVersionStrategy {
    private final Set<String> names = new HashSet<>();

    protected AbstractAppVersionStrategy(String... names) {
        Collections.addAll(this.names, names);
    }

    boolean recognizes(String name) {
        return name != null && names.contains(name.toLowerCase().trim());
    }
}
