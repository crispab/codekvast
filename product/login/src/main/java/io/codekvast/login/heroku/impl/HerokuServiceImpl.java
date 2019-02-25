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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.login.heroku.impl;

import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.security.CipherException;
import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.heroku.HerokuApiWrapper;
import io.codekvast.login.heroku.HerokuDetailsDAO;
import io.codekvast.login.heroku.HerokuException;
import io.codekvast.login.heroku.HerokuService;
import io.codekvast.login.heroku.model.*;
import io.codekvast.common.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HerokuServiceImpl implements HerokuService {

    private final CodekvastLoginSettings settings;
    private final CustomerService customerService;
    private final HerokuApiWrapper herokuApiWrapper;
    private final HerokuDetailsDAO herokuDetailsDAO;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HerokuProvisionResponse provision(HerokuProvisionRequest request) throws HerokuException {
        logger.debug("Handling {}", request);
        try {
            String licenseKey = customerService.addCustomer(CustomerService.AddCustomerRequest
                                                                .builder()
                                                                .source(CustomerService.Source.HEROKU)
                                                                .externalId(request.getUuid())
                                                                .name(request.getHeroku_id())
                                                                .plan(request.getPlan())
                                                                .build());

            String accessToken = exchangeOAuthGrant(request, licenseKey);

            fetchAppDetails(request.getUuid(), accessToken, licenseKey);

            Map<String, String> config = new HashMap<>();
            config.put("CODEKVAST_URL", settings.getHerokuCodekvastUrl());
            config.put("CODEKVAST_LICENSE_KEY", licenseKey);

            HerokuProvisionResponse response = HerokuProvisionResponse.builder()
                                                                      .id(request.getUuid())
                                                                      .config(config)
                                                                      .build();
            logger.debug("Returning {}", response);
            return response;
        } catch (Exception e) {
            throw new HerokuException("Could not execute " + request, e);
        }
    }

    private String exchangeOAuthGrant(HerokuProvisionRequest request, String licenseKey) throws CipherException {
        HerokuProvisionRequest.OAuthGrant oauthGrant = request.getOauth_grant();
        if (oauthGrant == null) {
            // Happens when you do `kensa test provision'
            return null;
        }

        if (herokuDetailsDAO.existsRow(licenseKey)) {
            // Happens if Heroku retries a request
            logger.info("OAuth tokens already fetched for {}", request);
            return null;
        }

        HerokuOAuthTokenResponse tokenResponse = herokuApiWrapper.exchangeGrantCode(oauthGrant);

        herokuDetailsDAO.saveTokens(tokenResponse, request.getCallback_url(), licenseKey);

        return tokenResponse.getAccess_token();
    }

    @Override
    public void changePlan(String externalId, HerokuChangePlanRequest request) throws HerokuException {
        logger.debug("Received {} for customers.externalId={}", request, externalId);
        try {
            customerService.changePlanForExternalId(externalId, request.getPlan());
        } catch (Exception e) {
            throw new HerokuException("Could not execute " + request + " for externalId '" + externalId + "'", e);
        }
    }

    @Override
    public void deprovision(String externalId) throws HerokuException {
        try {
            customerService.deleteCustomerByExternalId(externalId);
        } catch (Exception e) {
            throw new HerokuException("Could not deprovision externalId '" + externalId + "'", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Secured(Roles.ADMIN)
    public String getAccessTokenFor(Long customerId) throws CipherException {
        String accessToken = herokuDetailsDAO.getAccessToken(customerId);
        if (accessToken == null) {
            String refreshToken = herokuDetailsDAO.getRefreshToken(customerId);
            if (refreshToken != null) {
                HerokuOAuthTokenResponse response =
                    herokuApiWrapper.refreshAccessToken(refreshToken);
                accessToken = response.getAccess_token();
                Instant expiresAt = Instant.now().plusSeconds(response.getExpires_in());
                herokuDetailsDAO.updateAccessToken(customerId, accessToken, expiresAt);
            }
        }
        return accessToken;
    }

    @Override
    public String getCallbackUrlFor(Long customerId) {
        return herokuDetailsDAO.getCallbackUrl(customerId);
    }

    private void fetchAppDetails(String externalId, String accessToken, String licenseKey) {
        if (accessToken == null) {
            logger.info("Cannot get application details for licenseKey {}", licenseKey);
            return;
        }

        HerokuAppDetails appDetails = herokuApiWrapper.getAppDetails(externalId, accessToken);
        customerService.updateAppDetails(appDetails.getAppName(), appDetails.getOwnerEmail(), licenseKey);
    }
}
