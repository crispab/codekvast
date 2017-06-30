package io.codekvast.javaagent.scheduler;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.publishing.CodeBasePublisher;
import io.codekvast.javaagent.publishing.CodeBasePublisherFactory;
import io.codekvast.javaagent.publishing.InvocationDataPublisher;
import io.codekvast.javaagent.publishing.InvocationDataPublisherFactory;
import io.codekvast.javaagent.publishing.impl.JulAwareOutputCapture;
import io.codekvast.javaagent.publishing.impl.NoOpCodeBasePublisherImpl;
import io.codekvast.javaagent.publishing.impl.NoOpInvocationDataPublisherImpl;
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
    public OutputCapture output = new JulAwareOutputCapture();

    @Mock
    private ConfigPoller configPollerMock;

    @Mock
    private CodeBasePublisherFactory codeBasePublisherFactoryMock;

    @Mock
    private InvocationDataPublisherFactory invocationDataPublisherFactoryMock;

    @Mock
    private SystemClock systemClockMock;

    private AgentConfig config = AgentConfigFactory.createSampleAgentConfig();

    private CodeBasePublisher codeBasePublisher = new NoOpCodeBasePublisherImpl(config);

    private InvocationDataPublisher invocationDataPublisher = new NoOpInvocationDataPublisherImpl(config);

    private Scheduler scheduler;

    private final GetConfigResponse1 configResponse = GetConfigResponse1.sample()
        .toBuilder()
        .configPollIntervalSeconds(0)
        .configPollRetryIntervalSeconds(0)
        .codeBasePublisherCheckIntervalSeconds(0)
        .invocationDataPublisherIntervalSeconds(0)
        .build();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        scheduler =
            new Scheduler(config, configPollerMock, codeBasePublisherFactoryMock, invocationDataPublisherFactoryMock, systemClockMock);

        when(codeBasePublisherFactoryMock.create("no-op", config))
            .thenReturn(codeBasePublisher);

        when(invocationDataPublisherFactoryMock.create("no-op", config))
            .thenReturn(invocationDataPublisher);

        when(systemClockMock.currentTimeMillis()).thenReturn(System.currentTimeMillis());
    }

    @Test
    public void should_handle_shutdown_before_first_poll() throws Exception {
        scheduler.shutdown();
        verifyNoMoreInteractions(configPollerMock);
        output.expect(containsString("Scheduler stopped in 0 ms"));
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
        assertThat(invocationDataPublisher.getSequenceNumber(), is(2));
    }

    @Test
    public void should_do_first_publishing_soon_after_start() throws Exception {
        // given
        long now = System.currentTimeMillis();

        when(configPollerMock.doPoll()).thenReturn(
            configResponse
                .toBuilder()
                .configPollIntervalSeconds(5)
                .codeBasePublisherCheckIntervalSeconds(100)
                .invocationDataPublisherIntervalSeconds(100)
                .build());

        // when
        when(systemClockMock.currentTimeMillis()).thenReturn(now);
        scheduler.run();
        verify(configPollerMock, times(1)).doPoll();
        assertThat(codeBasePublisher.getSequenceNumber(), is(1));
        assertThat(invocationDataPublisher.getSequenceNumber(), is(1));

        when(systemClockMock.currentTimeMillis()).thenReturn(now + 2_000L);
        scheduler.run();
        verify(configPollerMock, times(1)).doPoll();
        assertThat(codeBasePublisher.getSequenceNumber(), is(1));
        assertThat(invocationDataPublisher.getSequenceNumber(), is(1));

        when(systemClockMock.currentTimeMillis()).thenReturn(now + 10_000L);
        scheduler.run();
        verify(configPollerMock, times(2)).doPoll();

        assertThat(codeBasePublisher.getSequenceNumber(), is(1));
        assertThat(invocationDataPublisher.getSequenceNumber(), is(1));

        when(systemClockMock.currentTimeMillis()).thenReturn(now + 20_000L);
        scheduler.run();
        verify(configPollerMock, times(3)).doPoll();
        assertThat(codeBasePublisher.getSequenceNumber(), is(1));
        assertThat(invocationDataPublisher.getSequenceNumber(), is(1));

        when(systemClockMock.currentTimeMillis()).thenReturn(now + 121_000L);
        scheduler.run();
        verify(configPollerMock, times(4)).doPoll();
        assertThat(codeBasePublisher.getSequenceNumber(), is(1));
        assertThat(invocationDataPublisher.getSequenceNumber(), is(2));
    }

    @Test
    public void should_handle_initial_poll_exceptions() throws Exception {
        when(configPollerMock.doPoll()).thenThrow(new IOException("Mock: No contact with server"));
        scheduler.run();
    }

    @Test
    public void should_retry_with_exponential_back_off() throws Exception {
        // given
        Scheduler.SchedulerState state = new Scheduler.SchedulerState("poller", systemClockMock)
            .initialize(10, 10);

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