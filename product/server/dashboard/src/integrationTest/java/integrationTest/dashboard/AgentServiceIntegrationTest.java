package integrationTest.dashboard;

import io.codekvast.common.aspects.DeadlockLoserDataAccessExceptionAspect;
import io.codekvast.common.bootstrap.CommonConfig;
import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.lock.Lock;
import io.codekvast.common.lock.LockManager;
import io.codekvast.common.lock.LockTemplate;
import io.codekvast.common.messaging.EventService;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.agent.impl.AgentDAO;
import io.codekvast.dashboard.agent.impl.AgentDAOImpl;
import io.codekvast.dashboard.agent.impl.AgentTransactionsImpl;
import io.codekvast.dashboard.agent.impl.AgentServiceImpl;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.javaagent.model.v2.GetConfigRequest2;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
@SpringBootTest(
    classes = {CommonConfig.class, AgentServiceImpl.class, AgentTransactionsImpl.class, AgentDAOImpl.class, LockTemplate.class, DeadlockLoserDataAccessExceptionAspect.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class AgentServiceIntegrationTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @MockBean
    private LockManager lockManager;

    @MockBean
    private AgentDAO agentDAO;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private EventService eventService;

    @MockBean
    private CodekvastDashboardSettings settings;

    @Inject
    private AgentService agentService;

    @Test
    public void should_retry_deadlock_loser_exception() {
        // given
        doThrow(new DeadlockLoserDataAccessException("Thrown by mock #1", null))
            .doThrow(new DeadlockLoserDataAccessException("Thrown by mock #2", null))
            .doNothing()
            .when(agentDAO).disableDeadAgents(anyLong(), anyString(), any());
        when(customerService.getCustomerDataByLicenseKey(anyString())).thenReturn(CustomerData.sample());
        when(customerService.registerAgentPoll(any(), any())).thenReturn(CustomerData.sample());
        when(lockManager.acquireLock(any())).thenReturn(Optional.of(Lock.forCustomer(1L).withAcquiredAt(Instant.now())));

        // when
        agentService.getConfig(GetConfigRequest2.sample());
    }

}
