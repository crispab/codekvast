package io.codekvast.common.messaging;

import io.codekvast.common.messaging.model.CodekvastEvent;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

/**
 * A wrapper for a received CodekvastEvent.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class CodekvastMessage {
    @NonNull String correlationId;
    @NonNull String messageId;
    @NonNull String senderApp;
    @NonNull Instant timestamp;
    @NonNull CodekvastEvent payload;
}
