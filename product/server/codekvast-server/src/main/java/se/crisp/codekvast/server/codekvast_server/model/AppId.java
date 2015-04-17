package se.crisp.codekvast.server.codekvast_server.model;

import lombok.Builder;
import lombok.Value;

/**
 * The identity of an application.
 *
 * Two applications with the same name executing in different JVMs are considered different applications.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class AppId {
    /**
     * What is the primary key of the organisations row?
     */
    long organisationId;

    /**
     * What is the primary key of the applications row?
     */
    long appId;

    /**
     * What is the primary key of the jvm_info row?
     */
    long jvmId;

    /**
     * What version of the app is running in this JVM?
     */
    String appVersion;
}
