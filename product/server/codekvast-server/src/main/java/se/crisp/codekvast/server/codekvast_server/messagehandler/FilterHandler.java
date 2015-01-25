package se.crisp.codekvast.server.codekvast_server.messagehandler;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Value;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.ApplicationCreatedEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.CustomerCreatedEvent;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for maintaining {@link FilterValues} objects for each active web socket user.
 * <p/>
 * Upon connection of a new user, an initial FilterValues object is built by querying other services. After that, the object is maintained
 * by subscribing to internal messages.
 * <p/>
 * Each time a FilterValues object is updated, the relevant user is notified via STOMP.
 *
 * @author Olle Hallin
 */
@Service
@Slf4j
public class FilterHandler {

    private final EventBus eventBus;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    private final Map<String, ActiveUser> sessionIdToActiveUser = new ConcurrentHashMap<>();
    private final Map<String, FilterValues> usernameToFilterValues = new ConcurrentHashMap<>();

    @Inject
    public FilterHandler(EventBus eventBus, SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.eventBus = eventBus;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
    }

    @PostConstruct
    void registerOnEventBus() {
        eventBus.register(this);
    }

    @PreDestroy
    void unregisterFromEventBus() {
        eventBus.unregister(this);
    }

    /**
     * A web socket user has logged in.
     */
    @Subscribe
    @AllowConcurrentEvents
    public void onSessionConnected(SessionConnectedEvent event) {
        log.debug("On {}", event.getClass().getSimpleName());
        MessageHeaders headers = event.getMessage().getHeaders();
        Principal user = SimpMessageHeaderAccessor.getUser(headers);
        if (user == null) {
            return;
        }

        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        String username = user.getName();
        sessionIdToActiveUser.put(sessionId, ActiveUser.builder().sessionId(sessionId).username(username).loggedInAt(new Date()).build());
        usernameToFilterValues.put(username, createInitialFilterValues(username));
    }

    /**
     * A web socket user leaves.
     */
    @Subscribe
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        log.debug("On {}", event.getClass().getSimpleName());
        String sessionId = event.getSessionId();
        ActiveUser removedUser = sessionId == null ? null : sessionIdToActiveUser.remove(sessionId);
        if (removedUser != null) {
            usernameToFilterValues.remove(removedUser.getUsername());
            log.debug("Removed {} {} (logged in at {})", sessionId, removedUser, removedUser.getLoggedInAt());
        }
    }

    /**
     * The JavaScript layer starts a STOMP subscription to filter values.
     *
     * @param principal The identity of the authenticated user.
     * @return The current FilterValues that the user shall use.
     */
    @SubscribeMapping("/filterValues")
    public FilterValues subscribeFilterValues(Principal principal) {
        String username = principal.getName();
        log.debug("'{}' is subscribing to filterValues", username);

        return usernameToFilterValues.get(username);
    }

    /**
     * A new customer has been created. Identify the relevant active users and update their filter values.
     */
    @Subscribe
    public void onCustomerCreated(CustomerCreatedEvent event) {
        log.debug("Handling {}", event);
        Collection<String> usernames = userService.getUsernamesWithRightsToViewCustomer(event.getCustomer().getName());
        for (String username : usernames) {
            FilterValues fv = usernameToFilterValues.get(username);
            if (fv != null) {
                String customerName = event.getCustomer().getName();
                log.debug("Adding customerName '{}' to filter values for {}", customerName, username);

                fv.getCustomerNames().add(customerName);
                messagingTemplate.convertAndSendToUser(username, "/queue/filterValues", fv);
            }
        }
    }

    /**
     * A new application has been created. Identify the relevant active users and update their filter values.
     */
    @Subscribe
    public void onApplicationCreated(ApplicationCreatedEvent event) {
        log.debug("Handling {}", event);
        Collection<String> usernames = userService.getUsernamesWithRightsToViewCustomer(event.getApplication().getCustomerName());
        for (String username : usernames) {
            FilterValues fv = usernameToFilterValues.get(username);
            if (fv != null) {
                String applicationName = event.getApplication().getName();
                log.debug("Adding application '{}' to filter values for {}", applicationName, username);

                fv.getApplications().add(applicationName);
                messagingTemplate.convertAndSendToUser(username, "/queue/filterValues", fv);
            }
        }
    }

    private FilterValues createInitialFilterValues(String username) {
        // TODO: implement
        Collection<String> customerNames = randomStrings(username + "-customer", 3);
        Collection<String> applications = randomStrings(username + "-app", 3);
        Collection<String> versions = randomStrings(username + "-v", 10);
        Collection<String> packages = randomStrings(username + "-pkg", 100);
        Collection<String> tags = randomStrings(username + "-tag", 10);

        return FilterValues.builder()
                           .customerNames(customerNames)
                           .applications(applications)
                           .versions(versions)
                           .packages(packages)
                           .tags(tags)
                           .build();
    }

    /**
     * Fake way to see that STOMP updates work.
     */
    @Scheduled(fixedRate = 5000L)
    public void sendFilterValuesToActiveUsers() {
        for (String username : usernameToFilterValues.keySet()) {
            log.debug("Sending filter values to '{}'", username);

            messagingTemplate.convertAndSendToUser(username, "/queue/filterValues", createInitialFilterValues(username));
        }
    }

    private Random random = new Random();

    private Collection<String> randomStrings(String prefix, int count) {
        Collection<String> result = new ArrayList<>();
        int c = (int) (count + random.nextGaussian() * count * 0.2d);

        for (int i = 0; i < c; i++) {
            String value = prefix + i;
            if (i >= 2) {
                // Don't append random values to the first couple of values, so that we can see that it is possible to select something
                // in the web page and that it remains selected when new filter values arrive.
                value += "-" + random.nextInt(100);
            }
            result.add(value);
        }
        return result;
    }

    /**
     * A value object containing everything that can be specified as filters in the web layer. It is sent as a STOMP message from the server
     * to the web layer as soon as there is a change in filter values.
     *
     * @author Olle Hallin
     */
    @Value
    @Builder
    public static class FilterValues {
        private Collection<String> customerNames;
        private Collection<String> applications;
        private Collection<String> versions;
        private Collection<String> packages;
        private Collection<String> tags;
    }

    /**
     * Binds a web socket session id to a username.
     *
     * @author Olle Hallin
     */
    @Value
    @Builder
    public static class ActiveUser {
        private final String sessionId;
        private final String username;
        private final Date loggedInAt;
    }
}
