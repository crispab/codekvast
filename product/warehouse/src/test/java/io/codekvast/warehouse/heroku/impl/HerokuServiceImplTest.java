package io.codekvast.warehouse.heroku.impl;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class HerokuServiceImplTest {

    private CodekvastSettings settings = new CodekvastSettings();

    @Mock
    private JdbcTemplate jdbcTemplate;

    private HerokuServiceImpl service;

    @Before
    public void beforeTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = new HerokuServiceImpl(settings, jdbcTemplate);
    }

    @Test
    public void should_generate_correct_sso_token() {
        // Example from https://devcenter.heroku.com/articles/add-on-single-sign-on

        // given
        String id = "123";
        settings.setHerokuApiSsoSalt("2f97bfa52ca102f8874716e2eb1d3b4920ad0be4");
        int timestamp = 1267597772;

        // when
        String token = service.makeSsoToken(id, timestamp);

        // then
        assertThat(token, is("bb466eb1d6bc345d11072c3cd25c311f21be130d"));
    }
}