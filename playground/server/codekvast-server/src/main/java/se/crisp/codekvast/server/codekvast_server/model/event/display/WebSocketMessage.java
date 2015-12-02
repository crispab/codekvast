package se.crisp.codekvast.server.codekvast_server.model.event.display;

import com.google.common.base.MoreObjects;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("applicationStatistics.size()", applicationStatistics == null ? "" : applicationStatistics.size())
                          .add("applications.size()", applications == null ? "" : applications.size())
                          .add("collectors.size()", collectors == null ? "" : collectors.size())
                          .add("environments.size()", environments == null ? "" : environments.size())
                          .toString();
    }
}
