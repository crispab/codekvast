package se.crisp.codekvast.web.service.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.web.model.RegistrationRequest;
import se.crisp.codekvast.web.service.RegistrationService;

import java.util.List;

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
    private static final String STATE_UNKNOWN = "unknown";

    @NonNull
    private final JdbcTemplate jdbcTemplate;

    @NonNull
    private final String mailChimpApiKey;

    @NonNull
    private final String mailChimpListId;

    @Autowired
    public RegistrationServiceImpl(JdbcTemplate jdbcTemplate,
                                   @Value("${codekvast.mailchimp.api-key}") String mailChimpApiKey,
                                   @Value("${codekvast.mailchimp.newsletter.list-id}") String mailChimpListId) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailChimpApiKey = mailChimpApiKey;
        this.mailChimpListId = mailChimpListId;
    }

    @Override
    @Transactional
    public void registerUser(RegistrationRequest request) {
        jdbcTemplate.update("INSERT INTO PEOPLE(EMAIL_ADDRESS, STATE) VALUES(?, ?)", request.getEmailAddress(), STATE_NEW);
    }

    @Scheduled(fixedDelay = 30_000L)
    @Transactional
    public void uploadNewPeopleToMailChimp() {
        List<String> newEmailAddresses =
                jdbcTemplate.queryForList("SELECT EMAIL_ADDRESS FROM PEOPLE WHERE STATE = ?", String.class, STATE_NEW);

        if (!newEmailAddresses.isEmpty()) {
            log.debug("Uploading {} new mail addresses to MailChimp", newEmailAddresses.size());
        }

        String newState = STATE_UPLOADED;
        try {
            uploadEmailAddressesToMailChimp(newEmailAddresses);
        } catch (UploadFailedException e) {
            newState = STATE_UNKNOWN;
        }

        jdbcTemplate.update("UPDATE PEOPLE SET STATE = ? WHERE EMAIL_ADDRESS IN ?", newState, newEmailAddresses);
    }

    private void uploadEmailAddressesToMailChimp(List<String> newEmailAddresses) throws UploadFailedException {

    }

    static class UploadFailedException extends Exception {
        public UploadFailedException(String message) {
            super(message);
        }
    }
}
