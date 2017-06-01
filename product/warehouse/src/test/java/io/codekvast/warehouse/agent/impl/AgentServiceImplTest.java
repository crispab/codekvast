package io.codekvast.warehouse.agent.impl;

import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.warehouse.agent.AgentService;
import io.codekvast.warehouse.agent.CustomerData;
import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.agent.LicenseViolationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class AgentServiceImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private JdbcTemplate jdbcTemplate;

    private final CodekvastSettings settings = new CodekvastSettings();
    private final GetConfigRequest1 request = GetConfigRequest1.sample();

    private AgentService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        settings.setQueuePath(temporaryFolder.getRoot());
        service = new AgentServiceImpl(settings, jdbcTemplate);

        Map<String, Object> map = new HashMap<>();
        map.put("id", 1L);
        map.put("plan", "test");

        when(jdbcTemplate.queryForMap(anyString(), anyString())).thenReturn(map);
    }

    @Test
    public void should_return_sensible_CustomerData() throws Exception {
        CustomerData data = service.getCustomerData("key");
        assertThat(data.getCustomerId(), is(1L));
        assertThat(data.getPlanName(), is("test"));
    }

    @Test
    public void should_return_sensible_defaults() throws Exception {
        GetConfigResponse1 response = service.getConfig(request);

        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=true"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=true"));
    }

    @Test(expected = LicenseViolationException.class)
    public void should_have_checked_licenseKey() throws Exception {
        when(jdbcTemplate.queryForMap(anyString(), anyString())).thenThrow(new EmptyResultDataAccessException(0));

        service.saveCodeBasePublication("key", "fingerprint", null);
    }

    @Test
    public void should_save_uploaded_codebase_no_license() throws Exception {
        String contents = "Dummy Code Base Publication";

        File resultingFile = service.saveCodeBasePublication("key",
                                                             "fingerprint",
                                                             new ByteArrayInputStream(contents.getBytes()));

        assertThat(resultingFile, notNullValue());
        assertThat(resultingFile.getName(), startsWith("codebase-"));
        assertThat(resultingFile.getName(), endsWith(".ser"));
        assertThat(resultingFile.exists(), is(true));
        assertThat(resultingFile.length(), is((long) contents.length()));
    }

    @Test(expected = NullPointerException.class)
    public void should_reject_null_codebase_licenseKey() throws Exception {
        service.saveCodeBasePublication(null, "fingerprint", null);
    }

    @Test(expected = NullPointerException.class)
    public void should_reject_null_invocation_data_licenseKey() throws Exception {
        service.saveInvocationDataPublication(null, "fingerprint", null);
    }

}