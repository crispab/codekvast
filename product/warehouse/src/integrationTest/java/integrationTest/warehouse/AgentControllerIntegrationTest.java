package integrationTest.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codekvast.javaagent.model.Endpoints;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.warehouse.CodekvastWarehouse;
import io.codekvast.warehouse.agent.AgentController;
import io.codekvast.warehouse.agent.AgentService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(SpringRunner.class)
@WebMvcTest(AgentController.class)
@ContextConfiguration(classes = CodekvastWarehouse.class)
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
        mvc.perform(post(Endpoints.AGENT_V1_POLL_CONFIG)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(request)))
           .andExpect(status().isOk());
    }
}
