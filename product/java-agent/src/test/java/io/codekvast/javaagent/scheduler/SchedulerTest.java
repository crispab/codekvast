package io.codekvast.javaagent.scheduler;

import io.codekvast.javaagent.publishing.CodeBasePublisher;
import io.codekvast.javaagent.publishing.CodeBasePublisherFactory;
import io.codekvast.javaagent.publishing.InvocationDataPublisher;
import io.codekvast.javaagent.publishing.InvocationDataPublisherFactory;
import io.codekvast.javaagent.publishing.impl.NoOpCodeBasePublisherImpl;
import io.codekvast.javaagent.publishing.impl.NoOpInvocationDataPublisherImpl;
import io.codekvast.javaagent.config.CollectorConfig;
import io.codekvast.javaagent.config.CollectorConfigFactory;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.rule.OutputCapture;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

/**
 * @author olle.hallin@crisp.se
 */
public class SchedulerTest {

    @Rule
    public OutputCapture output = new OutputCapture();

    @Mock
    private ConfigPoller configPollerMock;

    @Mock
    private CodeBasePublisherFactory codeBasePublisherFactoryMock;

    @Mock
    private InvocationDataPublisherFactory invocationDataPublisherFactoryMock;

    private CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();

    private CodeBasePublisher codeBasePublisher = new NoOpCodeBasePublisherImpl(config);

    private InvocationDataPublisher invocationDataPublisher = new NoOpInvocationDataPublisherImpl(config);

    private Scheduler scheduler;

    private final GetConfigResponse1 configResponse = GetConfigResponse1
        .builder()
        .configPollIntervalSeconds(0)
        .configPollRetryIntervalSeconds(0)
        .codeBasePublisherName("no-op")
        .codeBasePublisherConfig("enabled=true")
        .codeBasePublisherCheckIntervalSeconds(0)
        .invocationDataPublisherName("no-op")
        .invocationDataPublisherConfig("enabled=true")
        .invocationDataPublisherIntervalSeconds(0)
        .build();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        scheduler = new Scheduler(config, configPollerMock, codeBasePublisherFactoryMock, invocationDataPublisherFactoryMock);

        when(codeBasePublisherFactoryMock.create(anyString(), any(CollectorConfig.class)))
            .thenReturn(codeBasePublisher);

        when(invocationDataPublisherFactoryMock.create(anyString(), any(CollectorConfig.class)))
            .thenReturn(invocationDataPublisher);
    }

    @Test
    public void should_handle_shutdown_before_first_poll() throws Exception {
        scheduler.shutdown();
        verifyNoMoreInteractions(configPollerMock);
        output.expect(containsString("Stopping scheduler"));
        assertThat(codeBasePublisher.getSequenceNumber(), is(0));
        assertThat(invocationDataPublisher.getSequenceNumber(), is(0));
    }

    @Test
    public void should_handle_shutdown_after_being_started() throws Exception {
        // given
        when(configPollerMock.doPoll()).thenReturn(configResponse);

        // when
        scheduler.run();
        scheduler.run();
        scheduler.shutdown();

        // then
        verify(configPollerMock, times(2)).doPoll();
        verifyNoMoreInteractions(configPollerMock);

        assertThat(codeBasePublisher.getSequenceNumber(), is(1));
        assertThat(invocationDataPublisher.getSequenceNumber(), is(1));
    }

    @Test
    public void should_handle_initial_poll_exceptions() throws Exception {
        when(configPollerMock.doPoll()).thenThrow(new IOException("Mock: No contact with server"));
        scheduler.run();
    }

    @Test
    public void should_retry_with_exponential_back_off() throws Exception {
        // given
        Scheduler.SchedulerState state = new Scheduler.SchedulerState("poller").initialize(10, 10);
        assertThat(state.getRetryIntervalFactor(), is(1));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(1));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(1));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(1));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(1));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(1));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(2));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(4));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(8));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(16));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(16));

        state.scheduleNext();
        assertThat(state.getRetryIntervalFactor(), is(1));
    }
}