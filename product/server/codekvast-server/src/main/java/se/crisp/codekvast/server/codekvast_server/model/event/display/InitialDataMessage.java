package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.Value;

/**
 * A display object containing the initial data a user that logs in to the web interface needs.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class InitialDataMessage {
    CollectorStatusMessage collectorStatus;
    ApplicationStatisticsMessage applicationStatistics;
}
