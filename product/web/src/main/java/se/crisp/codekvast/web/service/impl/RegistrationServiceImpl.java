package se.crisp.codekvast.web.service.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.web.model.RegistrationRequest;
import se.crisp.codekvast.web.service.MailChimpException;
import se.crisp.codekvast.web.service.MailChimpService;
import se.crisp.codekvast.web.service.RegistrationService;

import java.util.List;
import java.util.Set;

/**
 * Responsible for uploading new email addresses to MailChimp.
 *
 * @author Olle Hallin
 */
@Service
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

    private static final String STATE_NEW = "new";
    private static final String STATE_UPLOADED = "uploaded";
    private static final String STATE_FAILED = "failed";

    @NonNull
    private final JdbcTemplate jdbcTemplate;

    @NonNull
    private final MailChimpService mailChimpService;

    @Autowired
    public RegistrationServiceImpl(JdbcTemplate jdbcTemplate, MailChimpService mailChimpService) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailChimpService = mailChimpService;
    }

    @Override
    @Transactional
    public void registerUser(RegistrationRequest request) {
        jdbcTemplate.update("INSERT INTO PEOPLE(EMAIL_ADDRESS, STATE) VALUES(?, ?)", request.getEmailAddress(), STATE_NEW);
    }

    @Override
    @Scheduled(cron = "${codekvast.mailchimp.cron}")
    @Transactional
    public void uploadNewPeopleToMailChimp() {
        List<String> newEmailAddresses =
                jdbcTemplate.queryForList("SELECT EMAIL_ADDRESS FROM PEOPLE WHERE STATE = ?", String.class, STATE_NEW);

        if (!newEmailAddresses.isEmpty()) {
            log.debug("Uploading {} new mail addresses to MailChimp", newEmailAddresses.size());

            try {
                MailChimpService.SubscribeToNewsletterResult result = mailChimpService.subscribeToNewsletter(newEmailAddresses);
                setState(result.getSubscribed(), STATE_UPLOADED);
                setState(result.getFailed(), STATE_FAILED);
            } catch (MailChimpException e) {
                log.error("Cannot subscribe to newsletter, will try again...", e);
            }
        }
    }

    private void setState(Set<String> emailAddresses, String newState) {
        for (String address : emailAddresses) {
            // H2 lacks the IN operator, so we must update one by one...
            jdbcTemplate.update("UPDATE PEOPLE SET STATE = ? WHERE EMAIL_ADDRESS = ?", newState, address);
        }
    }

}
