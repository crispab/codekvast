package io.codekvast.common.messaging;

/** @author olle.hallin@crisp.se */
public class DuplicateMessageIdException extends Exception {

  public DuplicateMessageIdException(String messageId) {
    super("The message with ID " + messageId + " was already processed");
  }
}
