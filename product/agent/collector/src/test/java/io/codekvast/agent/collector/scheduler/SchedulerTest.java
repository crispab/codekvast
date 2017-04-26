package io.codekvast.agent.collector.scheduler;

import io.codekvast.agent.collector.io.CodeBasePublisher;
import io.codekvast.agent.collector.io.CodeBasePublisherFactory;
import io.codekvast.agent.collector.io.InvocationDataPublisher;
import io.codekvast.agent.collector.io.InvocationDataPublisherFactory;
import io.codekvast.agent.collector.io.impl.NoOpCodeBasePublisherImpl;
import io.codekvast.agent.collector.io.impl.NoOpInvocationDataPublisherImpl;
import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.codebase.CodeBaseFingerprint;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import io.codekvast.agent.lib.model.v1.rest.GetConfigResponse1;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
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
        .invocationDataPublisherName("no-op")
        .invocationDataPublisherConfig("enabled=true")
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
    public void should_not_poll_if_not_started() throws Exception {
        scheduler.shutdown();
        verifyNoMoreInteractions(configPollerMock);
        output.expect(containsString("Stopping scheduler"));
    }

    @Test
    public void should_handle_shutdown_after_being_started() throws Exception {
        // given

        when(configPollerMock.doPoll(anyBoolean())).thenReturn(configResponse);
        when(configPollerMock.getCodeBaseFingerprint()).thenReturn(new CodeBaseFingerprint(1, "sha256"));

        // when
        scheduler.run();
        scheduler.run();
        scheduler.shutdown();

        // then
        verify(configPollerMock, times(1)).doPoll(true);
        verify(configPollerMock, times(1)).doPoll(false);
        verify(configPollerMock, times(2)).getCodeBaseFingerprint();
        verifyNoMoreInteractions(configPollerMock);
    }

    @Test
    public void should_publish_code_base_if_first_poll_says_it_is_needed() throws Exception {
        // given
        CodeBaseFingerprint fingerprint = new CodeBase(config).getFingerprint();

        when(configPollerMock.doPoll(true))
            .thenReturn(configResponse.toBuilder().codeBasePublishingNeeded(true).build());
        when(configPollerMock.getCodeBaseFingerprint()).thenReturn(fingerprint);

        scheduler.run();

        assertThat(codeBasePublisher.getPublicationCount(), is(1));
    }

    @Test
    public void should_not_publish_code_base_if_first_poll_says_it_is_not_needed() throws Exception {
        // given
        CodeBaseFingerprint fingerprint = new CodeBase(config).getFingerprint();

        when(configPollerMock.doPoll(true))
            .thenReturn(configResponse.toBuilder().codeBasePublishingNeeded(false).build());

        when(configPollerMock.getCodeBaseFingerprint()).thenReturn(fingerprint);

        scheduler.run();

        assertThat(codeBasePublisher.getPublicationCount(), is(0));
    }

    @Test
    public void should_handle_initial_poll_exceptions() throws Exception {
        when(configPollerMock.doPoll(anyBoolean())).thenThrow(new IOException("Mock: No contact with server"));
        scheduler.run();
    }

    @Test
    public void should_retry_with_exponential_back_off() throws Exception {
        // given
        Scheduler.SchedulerState state = new Scheduler.SchedulerState().initialize(10, 10);
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
        assertThat(state.getRetryIntervalFactor(), is(8));

        state.scheduleRetry();
        assertThat(state.getRetryIntervalFactor(), is(8));

        state.scheduleNext();
        assertThat(state.getRetryIntervalFactor(), is(1));

    }
}