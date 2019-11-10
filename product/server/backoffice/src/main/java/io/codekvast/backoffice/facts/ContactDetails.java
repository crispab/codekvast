package io.codekvast.backoffice.facts;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A fact about a customer's contact details.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class ContactDetails implements TransientFact {
    @NonNull private String contactEmail;
}
