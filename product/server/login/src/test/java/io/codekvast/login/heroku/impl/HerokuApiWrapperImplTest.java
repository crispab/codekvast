package io.codekvast.login.heroku.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.google.gson.Gson;
import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.heroku.model.HerokuAppDetails;
import io.codekvast.login.heroku.model.HerokuOAuthTokenResponse;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** @author olle.hallin@crisp.se */
@WireMockTest
class HerokuApiWrapperImplTest {
  private final Gson gson = new Gson();
  private final CodekvastLoginSettings settings = new CodekvastLoginSettings();
  private HerokuApiWrapperImpl herokuApiWrapper;

  @BeforeEach
  void beforeTest(WireMockRuntimeInfo wmRuntimeInfo) {

    settings.setHerokuApiBaseUrl(wmRuntimeInfo.getHttpBaseUrl());
    settings.setHerokuOAuthBaseUrl(wmRuntimeInfo.getHttpBaseUrl());
    settings.setHerokuOAuthClientSecret("clientSecret");

    herokuApiWrapper = new HerokuApiWrapperImpl(settings);
  }

  @Test
  void should_deserialize_exchange_grant_code_exchange_response() {
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                getClass()
                    .getResourceAsStream("/heroku/sample-exchange-grant-code-response.json")));

    // when
    HerokuOAuthTokenResponse response = gson.fromJson(reader, HerokuOAuthTokenResponse.class);

    // then
    assertThat(response.getAccess_token(), is("2af695e0-93e3-4821-ac2e-95f68435f128"));
    assertThat(response.getRefresh_token(), is("95a242fe-4c4a-4059-bc06-512de9672619"));
    assertThat(response.getToken_type(), is("Bearer"));
    assertThat(response.getExpires_in(), is(28800));
  }

  @Test
  void should_exchange_authorization_code_grant() throws IOException {
    // given
    String response = readResource("/heroku/sample-exchange-grant-code-response.json");

    HerokuProvisionRequest.OAuthGrant grant =
        HerokuProvisionRequest.OAuthGrant.builder()
            .code("code")
            .type("authorization_code")
            .expires_at(Instant.now().toString())
            .build();

    givenThat(post("/oauth/token").willReturn(okJson(response)));

    // when
    HerokuOAuthTokenResponse tokenResponse = herokuApiWrapper.exchangeGrantCode(grant);

    // then
    assertThat(tokenResponse.getAccess_token(), is("2af695e0-93e3-4821-ac2e-95f68435f128"));
  }

  @Test
  void should_refresh_token() throws IOException {
    // given
    String response = readResource("/heroku/sample-exchange-grant-code-response.json");

    givenThat(post("/oauth/token").willReturn(okJson(response)));

    // when
    HerokuOAuthTokenResponse tokenResponse = herokuApiWrapper.refreshAccessToken("refreshToken");

    // then
    assertThat(tokenResponse.getAccess_token(), is("2af695e0-93e3-4821-ac2e-95f68435f128"));
  }

  @Test
  void should_get_app_details() throws IOException {
    // given
    givenThat(
        get("/addons/some-external-id")
            .withHeader("Authorization", new EqualToPattern("Bearer some-access-token"))
            .withHeader("Accept", new EqualToPattern("application/vnd.heroku+json; version=3"))
            .willReturn(okJson(readResource("/heroku/sample-heroku-addons-response.json"))));

    givenThat(
        get("/apps/some-app-id")
            .withHeader("Authorization", new EqualToPattern("Bearer some-access-token"))
            .withHeader("Accept", new EqualToPattern("application/vnd.heroku+json; version=3"))
            .willReturn(okJson(readResource("/heroku/sample-heroku-apps-response.json"))));

    // when
    HerokuAppDetails appDetails =
        herokuApiWrapper.getAppDetails("some-external-id", "some-access-token");

    // then
    assertThat(
        appDetails,
        is(
            HerokuAppDetails.builder()
                .appName("some-app-name")
                .ownerEmail("some-owner-email")
                .build()));
  }

  private String readResource(String s) throws IOException {
    return Files.readString(Paths.get(getClass().getResource(s).getPath()));
  }
}
