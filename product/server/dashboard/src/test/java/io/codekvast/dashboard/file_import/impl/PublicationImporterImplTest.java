package io.codekvast.dashboard.file_import.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.codekvast.common.lock.Lock;
import io.codekvast.common.lock.LockManager;
import io.codekvast.common.lock.LockTemplate;
import io.codekvast.common.messaging.CorrelationIdHolder;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.file_import.CodeBaseImporter;
import io.codekvast.dashboard.file_import.InvocationDataImporter;
import io.codekvast.dashboard.file_import.PublicationImporter;
import io.codekvast.dashboard.metrics.AgentMetricsService;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import io.codekvast.javaagent.model.v3.CodeBasePublication3;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.assertj.core.util.Files;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;

/** @author olle.hallin@crisp.se */
public class PublicationImporterImplTest {

  @Mock private CodeBaseImporter codeBaseImporter;

  @Mock private InvocationDataImporter invocationDataImporter;

  @Mock private Validator validator;

  @Mock private AgentMetricsService metricsService;

  @Mock private AgentService agentService;

  @Mock private LockManager lockManager;

  private PublicationImporter publicationImporter;

  @Before
  public void beforeTest() {
    MockitoAnnotations.initMocks(this);
    when(agentService.getCorrelationIdFromPublicationFile(any()))
        .thenReturn(CorrelationIdHolder.generateNew());
    this.publicationImporter =
        new PublicationImporterImpl(
            codeBaseImporter,
            invocationDataImporter,
            validator,
            metricsService,
            agentService,
            new LockTemplate(lockManager));
    when(lockManager.acquireLock(any()))
        .thenReturn(
            Optional.of(
                Lock.forPublication(
                    new File(
                        "/intake/queue/invocations-29-683ce793-8bb8-4dc9-a494-cb2543aa7964.ser"))));
  }

  @Test
  public void should_import_CodeBasePublication3() throws URISyntaxException {
    // given
    File file = new File(getClass().getResource("/sample-publications/codebase-v2.ser").toURI());
    when(codeBaseImporter.importPublication(any(CodeBasePublication3.class))).thenReturn(true);

    // when
    boolean handled = publicationImporter.importPublicationFile(file);

    // then
    assertThat(handled, is(true));

    verify(codeBaseImporter).importPublication(any(CodeBasePublication3.class));
    verify(validator).validate(any());
    verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
  }

  @Test
  public void should_reject_CodeBasePublication3_when_DuplicateKeyException()
      throws URISyntaxException {
    // given
    File file = new File(getClass().getResource("/sample-publications/codebase-v2.ser").toURI());
    when(codeBaseImporter.importPublication(any(CodeBasePublication3.class)))
        .thenThrow(new DuplicateKeyException("Thrown by mock"));

    // when
    boolean handled = publicationImporter.importPublicationFile(file);

    // then
    assertThat(handled, is(false));

    verify(codeBaseImporter).importPublication(any(CodeBasePublication3.class));
    verify(validator).validate(any());
    verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
  }

  @Test
  public void should_swallow_CodeBasePublication2_when_InvalidClassException()
      throws URISyntaxException {
    // given
    File file =
        new File(
            getClass()
                .getResource("/sample-publications/codebase-v2-bad-serialVersionUID.ser")
                .toURI());

    // when
    boolean handled = publicationImporter.importPublicationFile(file);

    // then
    assertThat(handled, is(true));

    verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
  }

  @Test
  public void should_import_InvocationDataPublication2() throws URISyntaxException {
    // given
    File file = new File(getClass().getResource("/sample-publications/invocations-v2.ser").toURI());
    when(invocationDataImporter.importPublication(any(InvocationDataPublication2.class)))
        .thenReturn(true);

    // when
    boolean handled = publicationImporter.importPublicationFile(file);

    // then
    assertThat(handled, is(true));
    verify(invocationDataImporter).importPublication(any(InvocationDataPublication2.class));
    verify(validator).validate(any());
    verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
  }

  @Test
  public void should_not_process_locked_file() throws URISyntaxException {
    // given
    File file = new File(getClass().getResource("/sample-publications/invocations-v2.ser").toURI());
    when(lockManager.acquireLock(any())).thenReturn(Optional.empty());

    // when
    boolean handled = publicationImporter.importPublicationFile(file);

    // then
    assertThat(handled, is(false));
    verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
  }

  @Test
  public void should_reject_InvocationDataPublication2_when_DuplicateKeyException()
      throws URISyntaxException {
    // given
    File file = new File(getClass().getResource("/sample-publications/invocations-v2.ser").toURI());
    when(invocationDataImporter.importPublication(any(InvocationDataPublication2.class)))
        .thenThrow(new DuplicateKeyException("Thrown by mock"));

    // when
    boolean handled = publicationImporter.importPublicationFile(file);

    // then
    assertThat(handled, is(false));
    verify(invocationDataImporter).importPublication(any(InvocationDataPublication2.class));
    verify(validator).validate(any());
    verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
  }

  @Test
  public void should_ignore_unrecognized_content() throws IOException {
    // given
    File file = Files.newTemporaryFile();
    file.deleteOnExit();

    try (ObjectOutputStream oos =
        new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
      oos.writeObject("Hello, World!");
    }

    // when
    boolean handled = publicationImporter.importPublicationFile(file);

    // then
    assertThat(handled, is(false));
    verify(validator).validate(anyString());
    verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void should_handle_invalid_content() throws URISyntaxException {
    // given
    File file = new File(getClass().getResource("/sample-publications/invocations-v2.ser").toURI());
    when(validator.validate(any()))
        .thenReturn(Collections.singleton(mock(ConstraintViolation.class)));

    // when
    boolean handled = publicationImporter.importPublicationFile(file);

    // then
    assertThat(handled, is(true));
    verify(validator).validate(any());
    verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
  }
}
