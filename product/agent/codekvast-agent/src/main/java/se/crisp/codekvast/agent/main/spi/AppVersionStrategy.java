package se.crisp.codekvast.agent.main.spi;

/**
 * Strategy for how to obtain the version of an application.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
public interface AppVersionStrategy {
    /**
     * @return The name of the strategy.
     */
    String getName();

    /**
     * Use args for resolving the app version
     *
     * @param args The value of CollectorConfig.getAppVersionStrategy()
     * @return The resolved application version.
     */
    String getAppVersion(String[] args);
}
