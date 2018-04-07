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
package io.codekvast.login.heroku.impl;

import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.heroku.HerokuApiWrapper;
import io.codekvast.login.heroku.model.HerokuOAuthTokenResponse;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HerokuApiWrapperImpl implements HerokuApiWrapper {

    private final CodekvastLoginSettings settings;
    private final RestTemplateBuilder restTemplateBuilder;

    @Override
    public HerokuOAuthTokenResponse exchangeGrantCode(HerokuProvisionRequest.OAuthGrant grant) {
        logger.debug("Exchanging {}", grant);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("grant_type", grant.getType());
        bodyBuilder.part("code", grant.getCode());
        bodyBuilder.part("client_secret", settings.getHerokuOAuthClientSecret());

        ResponseEntity<HerokuOAuthTokenResponse> responseEntity =
            restTemplateBuilder.build().postForEntity("https://id.heroku.com/oauth/token",
                                                      new HttpEntity<>(bodyBuilder.build(), headers),
                                                      HerokuOAuthTokenResponse.class);
        HerokuOAuthTokenResponse response = responseEntity.getBody();
        logger.info("Received {}", response);
        return response;
    }
}
