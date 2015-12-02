package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;
import se.crisp.codekvast.server.codekvast_server.dao.DaemonDAO;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

import static org.mockito.Mockito.*;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(MockitoJUnitRunner.class)
public class StatisticsServiceImplTest {

    private static final AppId APP_ID1_1 = AppId.builder().organisationId(1).appId(1).jvmId(1).build();
    private static final AppId APP_ID1_2 = AppId.builder().organisationId(1).appId(1).jvmId(2).build();
    private static final AppId APP_ID2 = AppId.builder().organisationId(1).appId(2).jvmId(2).build();

    @Mock
    private EventBus eventBus;

    @Mock
    private DaemonDAO daemonDAO;

    private CodekvastSettings settings = new CodekvastSettings();

    private StatisticsServiceImpl statisticsService;

    @Before
    public void before() throws Exception {
        statisticsService = new StatisticsServiceImpl(daemonDAO, eventBus, settings);
        statisticsService.start();
    }

    @After
    public void after() throws Exception {
        statisticsService.shutdown();
    }

    @Test
    public void testRequestMultipleStats_withDelay() throws Exception {
        settings.setStatisticsDelayMillis(100);

        statisticsService.recalculateApplicationStatistics(APP_ID1_1);
        statisticsService.recalculateApplicationStatistics(APP_ID1_1);
        statisticsService.recalculateApplicationStatistics(APP_ID1_2);

        statisticsService.recalculateApplicationStatistics(APP_ID2);
        statisticsService.recalculateApplicationStatistics(APP_ID2);

        verifyNoMoreInteractions(daemonDAO);

        Thread.sleep(200);

        verify(daemonDAO, times(1)).recalculateApplicationStatistics(APP_ID1_1);
        verify(daemonDAO, times(1)).recalculateApplicationStatistics(APP_ID2);
    }

    @Test
    public void testRequestMultipleStats_withoutDelay() throws Exception {
        statisticsService.recalculateApplicationStatistics(APP_ID1_1);
        statisticsService.recalculateApplicationStatistics(APP_ID1_1);
        statisticsService.recalculateApplicationStatistics(APP_ID1_2);

        statisticsService.recalculateApplicationStatistics(APP_ID2);
        statisticsService.recalculateApplicationStatistics(APP_ID2);

        verify(daemonDAO, times(3)).recalculateApplicationStatistics(APP_ID1_1);
        verify(daemonDAO, times(2)).recalculateApplicationStatistics(APP_ID2);
    }
}
