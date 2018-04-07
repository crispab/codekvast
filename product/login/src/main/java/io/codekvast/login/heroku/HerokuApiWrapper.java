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
package io.codekvast.login.heroku;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.login.heroku.model.HerokuAppDetails;
import io.codekvast.login.heroku.model.HerokuOAuthTokenResponse;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;

/**
 * Wrapper for the REST interface towards the Heroku Partner API.
 * @author olle.hallin@crisp.se
 */
public interface HerokuApiWrapper {

    /**
     * Exchange an authorization_code grant to an access token and refresh token.
     *
     * @param grant Is received in the Heroku provisioning request.
     *
     * @return A HerokuOAuthTokenResponse object.
     */
    HerokuOAuthTokenResponse exchangeGrantCode(HerokuProvisionRequest.OAuthGrant grant);

    /**
     * Refresh the access token by presenting the refreshToken.
     *
     * @param refreshToken The permanent refresh token.
     *
     * @return A HerokuOAuthTokenResponse object.
     */
    HerokuOAuthTokenResponse refreshAccessToken(String refreshToken);

    /**
     * Retrieve Heroku app details for a certain app.
     * @param customerData The customer data.
     * @param accessToken The OAuth bearer token.
     * @return A HerokuAppDetails object.
     */
    HerokuAppDetails getAppDetails(CustomerData customerData, String accessToken);
}
