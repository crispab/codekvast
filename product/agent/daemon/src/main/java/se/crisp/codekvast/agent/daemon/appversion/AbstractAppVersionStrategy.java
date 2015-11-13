package se.crisp.codekvast.agent.daemon.appversion;

import java.util.HashSet;
import java.util.Set;

/**
 * @author olle.hallin@crisp.se
 */
abstract class AbstractAppVersionStrategy implements AppVersionStrategy {
    protected final Set<String> names = new HashSet<String>();

    protected AbstractAppVersionStrategy(String... names) {
        for (String name : names) {
            this.names.add(name);
        }
    }

    protected boolean recognizes(String name) {
        return name != null && names.contains(name.toLowerCase().trim());
    }
}
