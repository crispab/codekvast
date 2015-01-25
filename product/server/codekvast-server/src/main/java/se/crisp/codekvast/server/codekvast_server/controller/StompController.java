package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.event.web.FilterValues;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.ActiveUser;
import se.crisp.codekvast.server.codekvast_server.service.ActiveUserService;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * A Spring MVC Controller that handles STOMP messages from web clients.
 *
 *
 * @author Olle Hallin
 */
@Controller
@Slf4j
public class StompController {
    public static final String TOPIC_SIGNATURES = "/topic/signatures";

    private final UserService userService;
    private final ActiveUserService activeUserService;
    private final SimpMessagingTemplate messagingTemplate;

    @Inject
    public StompController(UserService userService, ActiveUserService activeUserService, SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.activeUserService = activeUserService;
        this.messagingTemplate = messagingTemplate;
    }

    @SubscribeMapping("/filterValues")
    public FilterValues subscribeFilterValues(Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' is subscribing to filterValues", username);

        FilterValues filterValues = createRandomFilterValues(username);

        log.debug("Returning {}", filterValues);
        return filterValues;
    }

    @Scheduled(fixedRate = 5000L)
    public void sendFilterValuesToActiveUsers() {
        for (ActiveUser activeUser : activeUserService.getActiveUsers()) {
            String username = activeUser.getUsername();
            log.debug("Sending filter values to '{}'", username);
            messagingTemplate.convertAndSendToUser(username, "/queue/filterValues", createRandomFilterValues(username));
        }
    }

    private FilterValues createRandomFilterValues(String username) {
        return FilterValues.builder()
                           .customerNames(randomStrings(username + "-customer", 3))
                           .applications(randomStrings(username + "-app", 3))
                           .versions(randomStrings(username + "-v", 10))
                           .packages(randomStrings(username + "-pkg", 100))
                           .tags(randomStrings(username + "-tag", 10))
                           .build();

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

    @SubscribeMapping("/signatures")
    public Collection<InvocationEntry> subscribeSignatures(Message message, Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("Received {} from '{}'", message, username);

        Collection<InvocationEntry> signatures = userService.getSignatures(null);

        log.debug("Returning {} signatures", signatures.size());
        return signatures;
    }
}
