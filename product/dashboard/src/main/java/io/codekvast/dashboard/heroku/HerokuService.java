/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.dashboard.heroku;

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
     */
    HerokuProvisionResponse provision(HerokuProvisionRequest request);

    /**
     * Request to change plan.
     *
     * @param externalId The value of {@link HerokuProvisionResponse#id}.
     * @param request    The change plan request.
     */
    void changePlan(String externalId, HerokuChangePlanRequest request);

    /**
     * Deprovision Codekvast from one Heroku app.
     *
     * @param externalId The value of {@link HerokuProvisionResponse#id}.
     */
    void deprovision(String externalId);

}
