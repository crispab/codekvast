package se.crisp.codekvast.web.service.impl;

import com.ecwid.mailchimp.MailChimpClient;
import com.ecwid.mailchimp.method.v2_0.lists.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.web.service.MailChimpException;
import se.crisp.codekvast.web.service.MailChimpService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Olle Hallin
 */
@Component
@Slf4j
public class MailChimpServiceImpl implements MailChimpService {

    @NonNull
    private final String apiKey;

    @NonNull
    private final String listId;

    @NonNull
    private final Boolean doubleOptIn;

    private final MailChimpClient mailChimpClient = new MailChimpClient();

    @Autowired
    public MailChimpServiceImpl(@Value("${codekvast.mailchimp.api-key}") String apiKey,
                                @Value("${codekvast.mailchimp.newsletter.list-id}") String listId,
                                @Value("${codekvast.mailchimp.newsletter.double-opt-in}") Boolean doubleOptIn) {
        this.apiKey = apiKey;
        this.listId = listId;
        this.doubleOptIn = doubleOptIn;
    }

    @Override
    public SubscribeToNewsletterResult subscribeToNewsletter(Collection<String> emailAddresses) throws MailChimpException {
        log.debug("Subscribing {} mail addresses to list {}", emailAddresses.size(), listId);

        BatchSubscribeMethod method = new BatchSubscribeMethod();
        method.apikey = apiKey;
        method.id = listId;
        method.batch = createSubscribeBatch(emailAddresses);
        method.double_optin = doubleOptIn;
        method.update_existing = true;
        method.replace_interests = false;

        try {
            BatchSubscribeResult mailchimpResult = mailChimpClient.execute(method);
            SubscribeToNewsletterResult result = convertResult(mailchimpResult);

            if (result.getFailed().isEmpty()) {
                log.info("{}", result);
            } else {
                log.warn("{}", result);
            }

            return result;
        } catch (Exception e) {
            throw new MailChimpException("Cannot subscribe to newsletter", e);
        }

    }

    private SubscribeToNewsletterResult convertResult(BatchSubscribeResult mailchimpResult) {
        SubscribeToNewsletterResult result = new SubscribeToNewsletterResult();

        for (Email email : mailchimpResult.adds) {
            result.getSubscribed().add(email.email);
        }
        for (Email email : mailchimpResult.updates) {
            result.getSubscribed().add(email.email);
        }
        for (BatchError error : mailchimpResult.errors) {
            result.getFailed().add(error.email.email);
        }

        return result;
    }

    private List<BatchSubscribeInfo> createSubscribeBatch(Collection<String> emailAddresses) {
        List<BatchSubscribeInfo> result = new ArrayList<>(emailAddresses.size());

        for (String address : emailAddresses) {
            BatchSubscribeInfo info = new BatchSubscribeInfo();
            info.email = new Email();
            info.email.email = address;
            result.add(info);
        }

        return result;
    }
}
