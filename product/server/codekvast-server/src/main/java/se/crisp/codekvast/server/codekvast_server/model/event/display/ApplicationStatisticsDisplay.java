package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A display object for one application.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class ApplicationStatisticsDisplay {

    @NonNull
    private String name;

    @NonNull
    private String version;

    int numSignatures;
    int numInvokedSignatures;
    int numStartupSignatures;
    int numTrulyDeadSignatures;
    int usageCycleSeconds;
    long firstDataReceivedAtMillis;
    long lastDataReceivedAtMillis;
    long fullUsageCycleEndsAtMillis;
}
