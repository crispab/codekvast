package io.codekvast.warehouse.security;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author olle.hallin@crisp.se
 */
public class HerokuSsoControllerTest {

    private CodekvastSettings settings = new CodekvastSettings();

    @Mock
    private JdbcTemplate jdbcTemplate;

    private HerokuSsoController controller;

    @Before
    public void beforeTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        controller = new HerokuSsoController(settings, jdbcTemplate, new SecurityHandler(settings));
    }


    @Test
    public void should_generate_correct_sso_token() {
        // Example from https://devcenter.heroku.com/articles/add-on-single-sign-on

        // given
        String id = "123";
        settings.setHerokuApiSsoSalt("2f97bfa52ca102f8874716e2eb1d3b4920ad0be4");
        int timestamp = 1267597772;

        // when
        String token = controller.makeHerokuSsoToken(id, timestamp);

        // then
        assertThat(token, is("bb466eb1d6bc345d11072c3cd25c311f21be130d"));
    }

}