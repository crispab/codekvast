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

}