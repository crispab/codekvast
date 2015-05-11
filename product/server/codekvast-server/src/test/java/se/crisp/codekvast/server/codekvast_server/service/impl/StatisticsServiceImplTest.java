package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

import static org.mockito.Mockito.*;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(MockitoJUnitRunner.class)
public class StatisticsServiceImplTest {

    private static final AppId APP_ID1 = AppId.builder().organisationId(1).appId(1).build();
    private static final AppId APP_ID2 = AppId.builder().organisationId(1).appId(2).build();

    @Mock
    private EventBus eventBus;

    @Mock
    private AgentDAO agentDAO;

    private CodekvastSettings settings = new CodekvastSettings();

    private StatisticsServiceImpl statisticsService;

    @Before
    public void before() throws Exception {
        settings.setStatisticsDelayMillis(100);
        statisticsService = new StatisticsServiceImpl(agentDAO, eventBus, settings);
        statisticsService.start();
    }

    @After
    public void after() throws Exception {
        statisticsService.shutdown();
    }

    @Test
    public void testRequestMultipleStats() throws Exception {
        statisticsService.recalculateApplicationStatistics(APP_ID1);
        statisticsService.recalculateApplicationStatistics(APP_ID1);
        statisticsService.recalculateApplicationStatistics(APP_ID1);

        statisticsService.recalculateApplicationStatistics(APP_ID2);
        statisticsService.recalculateApplicationStatistics(APP_ID2);

        verifyNoMoreInteractions(agentDAO);

        Thread.sleep(settings.getStatisticsDelayMillis() * 2);

        verify(agentDAO, times(1)).recalculateApplicationStatistics(APP_ID1);
        verify(agentDAO, times(1)).recalculateApplicationStatistics(APP_ID2);
    }
}
