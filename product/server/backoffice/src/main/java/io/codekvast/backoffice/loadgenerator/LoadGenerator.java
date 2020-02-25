package io.codekvast.backoffice.loadgenerator;

import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.model.InvocationDataReceivedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Helper class for locating a memory leak.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Profile("load-generator")
@RequiredArgsConstructor
public class LoadGenerator {

    private final EventService eventService;

    @Scheduled(fixedDelay = 10)
    public void sendEvent() {
        eventService.send(InvocationDataReceivedEvent.sample());
    }

}
