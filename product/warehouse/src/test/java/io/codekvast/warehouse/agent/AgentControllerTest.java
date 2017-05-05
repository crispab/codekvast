package io.codekvast.warehouse.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codekvast.javaagent.model.Endpoints;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.warehouse.customer.LicenseViolationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AgentControllerTest {

    @Mock
    private AgentService agentService;

    @InjectMocks
    private AgentController agentController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(agentController).build();
    }

    @Test
    public void should_have_mocks_injected() throws Exception {
        agentController.getConfig1(GetConfigRequest1.sample());
    }

    @Test
    public void getConfig1_should_reject_invalid_method() throws Exception {
        mockMvc.perform(get(Endpoints.AGENT_V1_POLL_CONFIG)
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void getConfig1_should_reject_invalid_media_type() throws Exception {
        mockMvc.perform(post(Endpoints.AGENT_V1_POLL_CONFIG)
                            .contentType(MediaType.TEXT_PLAIN))
               .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    public void getConfig1_should_reject_invalid_json() throws Exception {
        mockMvc.perform(post(Endpoints.AGENT_V1_POLL_CONFIG)
                            .content("invalid json")
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().isBadRequest());
    }

    @Test
    public void getConfig1_should_reject_invalid_request() throws Exception {
        mockMvc.perform(post(Endpoints.AGENT_V1_POLL_CONFIG)
                            .content(objectMapper.writeValueAsString(
                                GetConfigRequest1.sample()
                                                 .toBuilder()
                                                 .appName("")
                                                 .build()))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().isBadRequest());
    }

    @Test
    public void getConfig1_should_reject_invalid_licenseKey() throws Exception {
        when(agentService.getConfig(any(GetConfigRequest1.class))).thenThrow(new LicenseViolationException("foobar"));

        mockMvc.perform(post(Endpoints.AGENT_V1_POLL_CONFIG)
                            .content(objectMapper.writeValueAsString(GetConfigRequest1.sample()))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().isForbidden());
    }

    @Test
    public void getConfig1_should_accept_valid_request() throws Exception {
        when(agentService.getConfig(any(GetConfigRequest1.class))).thenReturn(
            GetConfigResponse1.sample().toBuilder().codeBasePublisherName("foobar").build());

        mockMvc.perform(post(Endpoints.AGENT_V1_POLL_CONFIG)
                            .content(objectMapper.writeValueAsString(GetConfigRequest1.sample()))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(jsonPath("$.codeBasePublisherName").value("foobar"));
    }

    @Test
    public void should_accept_upload_codebase_publication_when_valid_license() throws Exception {
        String licenseKey = "licenseKey";
        String fingerprint = "fingerprint";
        String originalFilename = "codekvast-codebase-9128371293719273.ser";

        MockMultipartFile publicationFile =
            new MockMultipartFile(Endpoints.AGENT_V1_PUBLICATION_FILE_PARAM,
                                  originalFilename,
                                  MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                  "CodeBasePublication".getBytes());

        mockMvc.perform(fileUpload(Endpoints.AGENT_V1_UPLOAD_CODEBASE)
                            .file(publicationFile)
                            .param(Endpoints.AGENT_V1_LICENSE_KEY_PARAM, licenseKey)
                            .param(Endpoints.AGENT_V1_FINGERPRINT_PARAM, fingerprint))
               .andExpect(status().isOk())
               .andExpect(content().string("OK"));

        verify(agentService).saveCodeBasePublication(eq(licenseKey), eq(fingerprint), any(InputStream.class));
    }

    @Test
    public void should_accept_upload_invocation_data_publication_when_valid_license() throws Exception {
        String licenseKey = "licenseKey";
        String fingerprint = "fingerprint";
        String originalFilename = "codekvast-invocations-9128371293719273.ser";

        MockMultipartFile multipartFile =
            new MockMultipartFile(Endpoints.AGENT_V1_PUBLICATION_FILE_PARAM,
                                  originalFilename,
                                  MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                  "InvocationDataPublication".getBytes());

        mockMvc.perform(fileUpload(Endpoints.AGENT_V1_UPLOAD_INVOCATION_DATA)
                            .file(multipartFile)
                            .param(Endpoints.AGENT_V1_LICENSE_KEY_PARAM, licenseKey)
                            .param(Endpoints.AGENT_V1_FINGERPRINT_PARAM, fingerprint))
               .andExpect(status().isOk())
               .andExpect(content().string("OK"));

        verify(agentService).saveInvocationDataPublication(eq(licenseKey), eq(fingerprint), any(InputStream.class));
    }
}