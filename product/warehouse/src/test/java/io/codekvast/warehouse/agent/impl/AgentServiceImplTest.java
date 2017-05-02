package io.codekvast.warehouse.agent.impl;

import io.codekvast.warehouse.agent.AgentService;
import io.codekvast.warehouse.agent.LicenseViolationException;
import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import io.codekvast.agent.api.model.v1.rest.GetConfigRequest1;
import io.codekvast.agent.api.model.v1.rest.GetConfigResponse1;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class AgentServiceImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final CodekvastSettings settings = new CodekvastSettings();
    private final AgentService service = new AgentServiceImpl(settings);
    private final GetConfigRequest1 request = GetConfigRequest1.sample();

    @Before
    public void setUp() {
        settings.setImportPath(temporaryFolder.getRoot());
    }

    @Test(expected = LicenseViolationException.class)
    public void should_throw_for_invalid_license_key() throws Exception {
        service.getConfig(request.toBuilder().licenseKey("-----").build());
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
    public void should_reject_uploaded_codebase_when_invalid_license() throws Exception {
        service.saveCodeBasePublication("-----", "fingerprint", null);
    }

    @Test
    public void should_save_uploaded_codebase_no_license() throws Exception {
        String contents = "Dummy Code Base Publication";

        File resultingFile = service.saveCodeBasePublication(null,
                                                             "fingerprint",
                                                             new ByteArrayInputStream(contents.getBytes()));

        assertThat(resultingFile, notNullValue());
        assertThat(resultingFile.getName(), startsWith("codebase-"));
        assertThat(resultingFile.getName(), endsWith(".ser"));
        assertThat(resultingFile.exists(), is(true));
        assertThat(resultingFile.length(), is((long) contents.length()));
    }

}