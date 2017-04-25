package io.codekvast.warehouse.agent.impl;

import io.codekvast.warehouse.agent.AgentService;
import io.codekvast.warehouse.agent.LicenseViolationException;
import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import org.junit.Before;
import org.junit.Test;
import io.codekvast.agent.lib.model.v1.rest.GetConfigRequest1;
import io.codekvast.agent.lib.model.v1.rest.GetConfigResponse1;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class AgentServiceImplTest {

    private final CodekvastSettings settings = new CodekvastSettings();
    private final AgentService service = new AgentServiceImpl(settings);
    private final GetConfigRequest1 request = GetConfigRequest1.sample();

    @Before
    public void setUp() throws Exception {
        settings.setImportPath(new File("/tmp/codekvast/.import"));
    }

    @Test(expected = LicenseViolationException.class)
    public void should_throw_for_invalid_license_key() throws Exception {
        service.getConfig(request.toBuilder().licenseKey("-----").build());

    }

    @Test
    public void should_return_sensible_defaults() throws Exception {
        GetConfigResponse1 response = service.getConfig(request);

        assertThat(response.getCodeBasePublisherName(), is("file-system"));
        assertThat(response.isCodeBasePublishingNeeded(), is(false));
        assertThat(response.getCodeBasePublisherConfig(), containsString("enabled=true"));
        assertThat(response.getCodeBasePublisherConfig(), containsString("targetFile="));
        assertThat(response.getCodeBasePublisherConfig(), containsString(settings.getImportPath().getAbsolutePath()));

        assertThat(response.getInvocationDataPublisherName(), is("file-system"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=true"));
    }
}