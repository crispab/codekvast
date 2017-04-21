package se.crisp.codekvast.warehouse.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import se.crisp.codekvast.agent.lib.model.rest.GetConfigRequest1;
import se.crisp.codekvast.agent.lib.model.rest.GetConfigResponse1;
import se.crisp.codekvast.warehouse.bootstrap.CodekvastSettings;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AgentControllerTest {

    @Mock
    private AgentService agentService;

    @Mock
    private CodekvastSettings settings;

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
    public void getConfig1_should_reject_invalid_media_type() throws Exception {
        mockMvc.perform(post("/agent/getConfig1")
                            .contentType(MediaType.TEXT_PLAIN))
               .andExpect(status().is4xxClientError());
    }

    @Test
    public void getConfig1_should_reject_invalid_body_json() throws Exception {
        mockMvc.perform(post("/agent/getConfig1")
                            .content("invalid json")
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().is4xxClientError());
    }

    @Test
    public void getConfig1_should_reject_invalid_request() throws Exception {
        mockMvc.perform(post("/agent/getConfig1")
                            .content(objectMapper.writeValueAsString(GetConfigRequest1.sample().toBuilder().licenseKey("").build()))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().is4xxClientError());
    }

    @Test
    public void getConfig1_should_accept_valid_request() throws Exception {
        when(agentService.getConfig(any(GetConfigRequest1.class))).thenReturn(
            GetConfigResponse1.sample().toBuilder().codeBasePublisherClass("foobar").build());

        mockMvc.perform(post("/agent/getConfig1")
                            .content(objectMapper.writeValueAsString(GetConfigRequest1.sample()))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(jsonPath("$.codeBasePublisherClass").value("foobar"));
    }
}