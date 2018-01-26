package integrationTest.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codekvast.dashboard.CodekvastDashboardApplication;
import io.codekvast.dashboard.agent.AgentController;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static io.codekvast.javaagent.model.Endpoints.Agent.V1_POLL_CONFIG;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(SpringRunner.class)
@WebMvcTest(AgentController.class)
@ContextConfiguration(classes = CodekvastDashboardApplication.class)
public class AgentControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("unused")
    @MockBean
    private AgentService agentService;

    @SuppressWarnings("unused")
    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    public void should_accept_post_to_agentPollConfig() throws Exception {
        GetConfigRequest1 request = GetConfigRequest1.sample();
        mvc.perform(post(V1_POLL_CONFIG)
                        .accept(APPLICATION_JSON_UTF8)
                        .contentType(APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk());
    }
}
