/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.login.heroku;

import io.codekvast.common.security.CipherException;
import io.codekvast.login.heroku.model.HerokuChangePlanRequest;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;
import io.codekvast.login.heroku.model.HerokuProvisionResponse;

import java.time.Instant;

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
     * @throws HerokuException should the request fail.
     */
    HerokuProvisionResponse provision(HerokuProvisionRequest request) throws HerokuException;

    /**
     * Request to change plan.
     *
     * @param externalId The value of {@link HerokuProvisionResponse#id}.
     * @param request    The change plan request.
     * @throws HerokuException should the request fail.
     */
    void changePlan(String externalId, HerokuChangePlanRequest request) throws HerokuException;

    /**
     * Deprovision Codekvast from one Heroku app.
     *
     * @param externalId The value of {@link HerokuProvisionResponse#id}.
     * @throws HerokuException should the request fail.
     */
    void deprovision(String externalId) throws HerokuException;

    /**
     * Get a valid access token for a certain customer.
     *
     * @param customerId The customerId
     * @return A valid access token. Returns null if not possible to refresh an expired access token.
     * @throws CipherException When failed to encrypt the access token.
     */
    String getAccessTokenFor(Long customerId) throws CipherException;

    /**
     *
     * @param customerId The customerId
     * @return The instant the access token expires. Returns null if customerId is not a Heroku customer.
     */
    Instant getAccessTokenExpiresAtFor(Long customerId) throws CipherException;

    /**
     * Get the callback URL for a certain customer.
     *
     * @param customerId The customerId
     * @return The callback URL.
     */
    String getCallbackUrlFor(Long customerId);
}
