package io.codekvast.warehouse.heroku;

/**
 * Service for handling Heroku provisioning, deprovisioning and plan changes.
 *
 * @author olle.hallin@crisp.se
 */
public interface HerokuService {
    /**
     * Provision Codekvast for one Heroku app.
     *
     * @param request The provisioning request sent by Heroku.
     * @return The response that Heroku will forward to the app developer.
     * @throws HerokuException If failed to satisfy the request.
     */
    HerokuProvisionResponse provision(HerokuProvisionRequest request) throws HerokuException;

    /**
     * Deprovision Codekvast from one Heroku app.
     *
     * @param externalId The value of {@link HerokuProvisionResponse#id}.
     * @throws HerokuException If failed to deprovision, try again later.
     */
    void deprovision(String externalId) throws HerokuException;
}
