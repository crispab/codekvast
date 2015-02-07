package se.crisp.codekvast.web.service;

import se.crisp.codekvast.web.model.RegistrationRequest;

/**
 * @author olle.hallin@crisp.se
 */
public interface RegistrationService {
    void registerUser(RegistrationRequest request);

    void uploadNewPeopleToMailChimp();
}
