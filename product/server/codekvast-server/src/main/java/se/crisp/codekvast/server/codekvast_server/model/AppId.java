package se.crisp.codekvast.server.codekvast_server.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * The identity of an application.
 *
 * The JVM id is not included in equals() and hashCode()
 *
 * @author olle.hallin@crisp.se
 */
@Value
@EqualsAndHashCode(exclude = "jvmId")
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
     * What version of the app is running in this JVM?
     */
    String appVersion;

    /**
     * What is the primary key of the jvm_info row?
     *
     * NOTE: jvmId is not included in equals() and hashCode()
     */
    long jvmId;

}
