package io.codekvast.backoffice.rabbit;

import io.codekvast.common.messaging.AbstractCodekvastEventListener;
import io.codekvast.common.messaging.impl.MessageIdRepository;
import io.codekvast.common.messaging.model.CodekvastEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class CodekvastEventListener extends AbstractCodekvastEventListener {

    public CodekvastEventListener(MessageIdRepository messageIdRepository) {
        super(messageIdRepository);
    }

    @Override
    public void onCodekvastEvent(CodekvastEvent event) throws Exception {
        logger.debug("Received {}", event);
        // TODO: handle event
    }
}
