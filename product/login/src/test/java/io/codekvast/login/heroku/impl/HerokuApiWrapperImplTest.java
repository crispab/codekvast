package io.codekvast.login.heroku.impl;

import com.google.gson.Gson;
import io.codekvast.login.heroku.model.HerokuOAuthTokenResponse;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class HerokuApiWrapperImplTest {
    private final Gson gson = new Gson();

    @Test
    public void should_deserialize_exchange_grant_code_exchange_response() {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/heroku/sample-exchange-grant-code-response.json")));

        // when
        HerokuOAuthTokenResponse response = gson.fromJson(reader, HerokuOAuthTokenResponse.class);

        // then
        assertThat(response.getAccess_token(), is("2af695e0-93e3-4821-ac2e-95f68435f128"));
        assertThat(response.getRefresh_token(), is("95a242fe-4c4a-4059-bc06-512de9672619"));
        assertThat(response.getToken_type(), is("Bearer"));
        assertThat(response.getExpires_in(), is(28800));
    }
}