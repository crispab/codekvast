package se.crisp.codekvast.web.service;

import se.crisp.codekvast.web.model.RegistrationRequest;

/**
 * @author Olle Hallin
 */
public interface RegistrationService {
    void registerUser(RegistrationRequest request);
}
