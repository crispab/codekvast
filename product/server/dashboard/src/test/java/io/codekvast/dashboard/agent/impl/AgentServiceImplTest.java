package io.codekvast.dashboard.agent.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.messaging.CorrelationIdHolder;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.dashboard.metrics.AgentMetricsService;
import io.codekvast.dashboard.model.PublicationType;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AgentServiceImplTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock private CustomerService customerService;

  @Mock private AgentDAO agentDAO;

  @Mock private AgentMetricsService agentMetricsService;
  private final CustomerData customerData = CustomerData.sample();

  private AgentService service;

  @Before
  public void beforeTest() {
    MockitoAnnotations.initMocks(this);

    CodekvastDashboardSettings settings = new CodekvastDashboardSettings();
    settings.setFileImportQueuePath(temporaryFolder.getRoot());

    when(customerService.getCustomerDataByLicenseKey(anyString())).thenReturn(customerData);

    service =
        new AgentServiceImpl(
            settings,
            customerService,
            agentDAO,
            mock(AgentStateManager.class),
            agentMetricsService);
  }

  @Test
  public void should_close_inputStream_after_throwing() throws Exception {
    // given
    int publicationSize = 4711;
    doThrow(new LicenseViolationException("stub"))
        .when(customerService)
        .assertPublicationSize(any(CustomerData.class), eq(publicationSize));
    val inputStream = mock(InputStream.class);

    try {
      // when
      service.savePublication(
          PublicationType.CODEBASE, "key", "fingerprint", publicationSize, inputStream);

      // then
      fail("Expected a LicenseViolationException");
    } catch (LicenseViolationException expected) {
      // Expected outcome
    } finally {
      verify(inputStream).close();
    }
  }

  @Test
  public void should_close_inputStream_after_not_throwing() throws Exception {
    // given
    val inputStream = mock(InputStream.class);

    // when
    service.savePublication(PublicationType.CODEBASE, "key", "fingerprint", 4711, inputStream);

    // then
    verify(inputStream).close();
  }

  @Test
  public void should_save_uploaded_codebase() throws Exception {
    // given
    String contents = "Dummy Code Base Publication";

    // when
    File resultingFile =
        service.savePublication(
            PublicationType.CODEBASE,
            "key",
            "fingerprint",
            1000,
            new ByteArrayInputStream(contents.getBytes()));

    // then
    assertThat(resultingFile, notNullValue());
    assertThat(
        service.getPublicationTypeFromPublicationFile(resultingFile), is(PublicationType.CODEBASE));
    assertThat(resultingFile.getName(), startsWith("codebase-"));
    assertThat(resultingFile.getName(), endsWith(".ser"));
    assertThat(resultingFile.exists(), is(true));
    assertThat(resultingFile.length(), is((long) contents.length()));
  }

  @Test
  public void should_save_uploaded_invocations() throws Exception {
    // given
    String contents = "Dummy Invocations Publication";

    // when
    File resultingFile =
        service.savePublication(
            PublicationType.INVOCATIONS,
            "key",
            "fingerprint",
            1000,
            new ByteArrayInputStream(contents.getBytes()));

    // then
    assertThat(resultingFile, notNullValue());
    assertThat(resultingFile.getName(), startsWith("invocations-"));
    assertThat(resultingFile.getName(), endsWith(".ser"));
    assertThat(resultingFile.exists(), is(true));
    assertThat(resultingFile.length(), is((long) contents.length()));
  }

  @Test(expected = NullPointerException.class)
  public void should_reject_null_licenseKey() throws Exception {
    service.savePublication(PublicationType.CODEBASE, null, "fingerprint", 0, null);
  }

  @Test
  public void should_build_file_name_with_correlationId() {
    // given
    String correlationId = CorrelationIdHolder.generateNew();

    // when
    File file = service.generatePublicationFile(PublicationType.CODEBASE, 17L, correlationId);

    // then
    assertThat(file.getName(), containsString(correlationId));

    // when
    String correlationId2 = service.getCorrelationIdFromPublicationFile(file);

    // then
    assertThat(correlationId2, is(correlationId));
  }

  @Test
  public void should_count_agent_polls() {

    // when
    service.getConfig(GetConfigRequest1.sample());

    // then
    verify(agentMetricsService, times(1)).countAgentPoll();
  }
}
