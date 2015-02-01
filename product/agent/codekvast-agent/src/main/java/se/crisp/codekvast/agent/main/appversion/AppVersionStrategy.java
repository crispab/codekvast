package se.crisp.codekvast.agent.main.appversion;

import java.io.File;
import java.util.Collection;

/**
 * Strategy for how to obtain the version of an application.
 *
 * @author Olle Hallin <olle.hallin@crisp.se>
 */
public interface AppVersionStrategy {

    String UNKNOWN_VERSION = "<unknown>";

    /**
     * Can this strategy handle these args?
     * @param args The white-space separated value from {@link se.crisp.codekvast.agent.config.CollectorConfig#getAppVersion()}
     * @return true if-and-only-if the strategy recognizes the args.
     */
    boolean canHandle(String[] args);

    /**
     * Use args for resolving the app version
     *
     * @param codeBases The locations of the code base.
     * @param args The value of CollectorConfig.getAppVersionStrategy()
     * @return The resolved application version.
     */
    String resolveAppVersion(Collection<File> codeBases, String[] args);
}
