/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
package io.codekvast.login.heroku.impl;

import com.jayway.jsonpath.JsonPath;
import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.heroku.HerokuApiWrapper;
import io.codekvast.login.heroku.model.HerokuAppDetails;
import io.codekvast.login.heroku.model.HerokuOAuthTokenResponse;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/** @author olle.hallin@crisp.se */
@Component
@RequiredArgsConstructor
@Slf4j
public class HerokuApiWrapperImpl implements HerokuApiWrapper {

  private final CodekvastLoginSettings settings;

  // TODO inject the RestTemplateBuilder to get metrics
  private final RestTemplate restTemplate =
      new RestTemplateBuilder()
          .messageConverters(
              new FormHttpMessageConverter(),
              new StringHttpMessageConverter(),
              new GsonHttpMessageConverter())
          .build();

  @Override
  public HerokuOAuthTokenResponse exchangeGrantCode(HerokuProvisionRequest.OAuthGrant grant) {
    logger.debug("Exchanging {}", grant);

    MultiValueMap<String, String> form =
        createGetOAuthTokenForm("authorization_code", "code", grant.getCode());

    return getHerokuOAuthTokenResponse(form);
  }

  @Override
  public HerokuOAuthTokenResponse refreshAccessToken(String refreshToken) {
    logger.debug("Refreshing an access token");

    MultiValueMap<String, String> form =
        createGetOAuthTokenForm("refresh_token", "refresh_token", refreshToken);

    return getHerokuOAuthTokenResponse(form);
  }

  @Override
  public HerokuAppDetails getAppDetails(String externalId, String accessToken) {
    logger.debug("Getting Heroku app details for {}", externalId);
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", "application/vnd.heroku+json; version=3");
    headers.set("Authorization", "Bearer " + accessToken);

    String url = String.format("%s/addons/%s", settings.getHerokuApiBaseUrl(), externalId);
    ResponseEntity<String> json =
        restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
    String appId = JsonPath.read(json.getBody(), "$.app.id");
    String appName = JsonPath.read(json.getBody(), "$.app.name");

    url = String.format("%s/apps/%s", settings.getHerokuApiBaseUrl(), appId);
    json =
        restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), String.class);

    String ownerEmail = JsonPath.read(json.getBody(), "$.owner.email");

    return HerokuAppDetails.builder().appName(appName).ownerEmail(ownerEmail).build();
  }

  private MultiValueMap<String, String> createGetOAuthTokenForm(
      String grantType, String key, String value) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", grantType);
    form.add(key, value);
    form.add("client_secret", settings.getHerokuOAuthClientSecret());
    return form;
  }

  private HerokuOAuthTokenResponse getHerokuOAuthTokenResponse(
      MultiValueMap<String, String> requestForm) {
    val headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    String url = String.format("%s/oauth/token", settings.getHerokuOAuthBaseUrl());
    val responseEntity =
        restTemplate.postForEntity(
            url, new HttpEntity<>(requestForm, headers), HerokuOAuthTokenResponse.class);
    val response = responseEntity.getBody();
    logger.info("Received {}", response);
    return response;
  }
}
