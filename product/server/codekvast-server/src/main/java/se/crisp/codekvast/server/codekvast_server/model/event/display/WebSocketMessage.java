package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.Value;

import java.util.Collection;

/**
 * A display object containing the initial data a user that logs in to the web interface needs.
 *
 * It is also sent automatically over the web socket whenever any data is updated.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class WebSocketMessage {
    /**
     * Which usernames should be broadcast this message (if logged in)?
     */
    Collection<String> usernames;

    Collection<ApplicationStatisticsDisplay> applicationStatistics;
    Collection<ApplicationDisplay> applications;
    Collection<CollectorDisplay> collectors;
    Collection<EnvironmentDisplay> environments;
}
