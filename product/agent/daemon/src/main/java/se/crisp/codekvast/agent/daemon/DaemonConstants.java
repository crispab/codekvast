package se.crisp.codekvast.agent.daemon;

import se.crisp.codekvast.server.daemon_api.DaemonApi;

/**
 * Constants used in CodekvastDaemon
 *
 * @author olle.hallin@crisp.se
 */
public interface DaemonConstants {
    String HTTP_POST_PROFILE = DaemonApi.SPRING_PROFILE_NAME;
    String LOCAL_WAREHOUSE_PROFILE = "localWarehouse";
}

