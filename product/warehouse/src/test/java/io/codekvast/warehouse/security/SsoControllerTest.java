package io.codekvast.warehouse.security;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.security.impl.SecurityServiceImpl;
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
public class SsoControllerTest {

    private CodekvastSettings settings = new CodekvastSettings();

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SsoController controller;

    @Before
    public void beforeTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        controller = new SsoController(settings, jdbcTemplate, new SecurityServiceImpl(settings));
    }


    @Test
    public void should_generate_correct_heroku_sso_token() {
        // Example data from https://devcenter.heroku.com/articles/add-on-single-sign-on

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