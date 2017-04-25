package io.codekvast.agent.collector.scheduler;

import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author olle.hallin@crisp.se
 */
public class SchedulerTest {

    @Mock
    private ConfigPoller configPollerMock;

    private CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();

    private Scheduler scheduler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        scheduler = new Scheduler(config, configPollerMock);
    }

    @Test
    public void should_handle_shutdown_without_being_started() throws Exception {
        scheduler.shutdown();
        verify(configPollerMock, times(0)).doPoll(anyBoolean());
    }
}