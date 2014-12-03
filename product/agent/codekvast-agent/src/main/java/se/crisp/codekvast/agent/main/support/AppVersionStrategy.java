package se.crisp.codekvast.agent.main.support;

import java.net.URI;

/**
 * Strategy for how to obtain the version of an application.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
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
     * @param codeBaseUri The location of the code base.
     * @param args The value of CollectorConfig.getAppVersionStrategy()
     * @return The resolved application version.
     */
    String resolveAppVersion(URI codeBaseUri, String[] args);
}
