package io.codekvast.dashboard.agent;

import com.google.gson.Gson;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;

import static io.codekvast.javaagent.model.Endpoints.Agent.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AgentControllerTest {

    @Mock
    private AgentService agentService;

    @InjectMocks
    private AgentController agentController;

    private MockMvc mockMvc;

    private final Gson gson = new Gson();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(agentController)
                                      .setMessageConverters(new GsonHttpMessageConverter(), new StringHttpMessageConverter())
                                      .build();
    }

    @Test
    public void should_have_mocks_injected() {
        agentController.getConfig1(GetConfigRequest1.sample());
    }

    @Test
    public void getConfig1_should_reject_invalid_method() throws Exception {
        mockMvc.perform(get(V1_POLL_CONFIG).contentType(APPLICATION_JSON_UTF8))
               .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void getConfig1_should_reject_invalid_media_type() throws Exception {
        mockMvc.perform(post(V1_POLL_CONFIG).contentType(TEXT_PLAIN))
               .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    public void getConfig1_should_reject_invalid_json() throws Exception {
        mockMvc.perform(post(V1_POLL_CONFIG)
                            .content("invalid json")
                            .contentType(APPLICATION_JSON_UTF8))
               .andExpect(status().isBadRequest());
    }

    @Test
    public void getConfig1_should_reject_invalid_request() throws Exception {
        mockMvc.perform(post(V1_POLL_CONFIG)
                            .content(gson.toJson(
                                GetConfigRequest1.sample()
                                                 .toBuilder()
                                                 .appName("")
                                                 .build()))
                            .contentType(APPLICATION_JSON_UTF8))
               .andExpect(status().isBadRequest());
    }

    @Test
    public void getConfig1_should_reject_invalid_licenseKey() throws Exception {
        when(agentService.getConfig(any(GetConfigRequest1.class))).thenThrow(new LicenseViolationException("foobar"));

        mockMvc.perform(post(V1_POLL_CONFIG)
                            .content(gson.toJson(GetConfigRequest1.sample()))
                            .contentType(APPLICATION_JSON_UTF8))
               .andExpect(status().isForbidden());
    }

    @Test
    public void getConfig1_should_accept_valid_request() throws Exception {
        when(agentService.getConfig(any(GetConfigRequest1.class))).thenReturn(
            GetConfigResponse1.sample().toBuilder().codeBasePublisherName("foobar").build());

        mockMvc.perform(post(V1_POLL_CONFIG)
                            .content(gson.toJson(GetConfigRequest1.sample()))
                            .contentType(APPLICATION_JSON_UTF8))
               .andExpect(status().isOk())
               .andExpect(content().contentType(APPLICATION_JSON_UTF8))
               .andExpect(jsonPath("$.codeBasePublisherName").value("foobar"));
    }

    @Test
    public void should_accept_upload_codebase_publication1_when_valid_license() throws Exception {
        assertUploadPublication(AgentService.PublicationType.CODEBASE, V1_UPLOAD_CODEBASE);
    }

    @Test
    public void should_accept_upload_codebase_publication2_when_valid_license() throws Exception {
        assertUploadPublication(AgentService.PublicationType.CODEBASE, V2_UPLOAD_CODEBASE);
    }

    @Test
    public void should_accept_upload_invocation_data_publication1_when_valid_license() throws Exception {
        assertUploadPublication(AgentService.PublicationType.INVOCATIONS, V1_UPLOAD_INVOCATION_DATA);
    }

    @Test
    public void should_accept_upload_invocation_data_publication2_when_valid_license() throws Exception {
        assertUploadPublication(AgentService.PublicationType.INVOCATIONS, V2_UPLOAD_INVOCATION_DATA);
    }

    private void assertUploadPublication(AgentService.PublicationType publicationType, String endpoint) throws Exception {
        String licenseKey = "licenseKey";
        String fingerprint = "fingerprint";
        int publicationSize = 10000;
        String originalFilename = String.format("codekvast-%s-9128371293719273.ser", publicationType);

        MockMultipartFile multipartFile =
            new MockMultipartFile(PARAM_PUBLICATION_FILE,
                                  originalFilename,
                                  APPLICATION_OCTET_STREAM_VALUE,
                                  ("PublicationContent-" + publicationType).getBytes());

        mockMvc.perform(multipart(endpoint)
                            .file(multipartFile)
                            .param(PARAM_LICENSE_KEY, licenseKey)
                            .param(PARAM_FINGERPRINT, fingerprint)
                            .param(PARAM_PUBLICATION_SIZE, publicationSize + ""))
               .andExpect(status().isOk())
               .andExpect(content().string("OK"));

        verify(agentService).savePublication(eq(publicationType), eq(licenseKey), eq(publicationSize), any(InputStream.class));
    }
}