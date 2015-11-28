package se.crisp.codekvast.web.service;

import lombok.Data;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author olle.hallin@crisp.se
 */
public interface MailChimpService {
    /**
     * Subscribe a collection of email addresses to the Codekvast Newsletter
     *
     * @param emailAddresses The new email addresses
     * @return An object that describes which addresses that were successfully subscribed and which failed.
     * @throws MailChimpException Should the API call fail entirely.
     */
    SubscribeToNewsletterResult subscribeToNewsletter(Collection<String> emailAddresses) throws MailChimpException;

    @Data
    class SubscribeToNewsletterResult {
        private Set<String> subscribed = new TreeSet<>();
        private Set<String> failed = new TreeSet<>();
    }
}
