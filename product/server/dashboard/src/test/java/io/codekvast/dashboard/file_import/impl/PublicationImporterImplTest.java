package io.codekvast.dashboard.file_import.impl;

import io.codekvast.common.lock.LockManager;
import io.codekvast.dashboard.file_import.CodeBaseImporter;
import io.codekvast.dashboard.file_import.InvocationDataImporter;
import io.codekvast.dashboard.file_import.PublicationImporter;
import io.codekvast.dashboard.metrics.IntakeMetricsService;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import io.codekvast.javaagent.model.v3.CodeBasePublication3;
import org.assertj.core.util.Files;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author olle.hallin@crisp.se
 */
public class PublicationImporterImplTest {

    @Mock
    private CodeBaseImporter codeBaseImporter;

    @Mock
    private InvocationDataImporter invocationDataImporter;

    @Mock
    private Validator validator;

    @Mock
    private IntakeMetricsService metricsService;

    @Mock
    private LockManager lockManager;

    private PublicationImporter publicationImporter;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        when(lockManager.acquireLock(LockManager.Lock.IMPORT)).thenReturn(Optional.of(LockManager.Lock.IMPORT));
        this.publicationImporter = new PublicationImporterImpl(codeBaseImporter, invocationDataImporter, validator, metricsService, lockManager);
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
        verify(lockManager).acquireLock(LockManager.Lock.IMPORT);
        verify(lockManager).releaseLock(LockManager.Lock.IMPORT);
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
    }

    @Test
    public void should_reject_CodeBasePublication3_when_DuplicateKeyException() throws URISyntaxException {
        // given
        File file = new File(getClass().getResource("/sample-publications/codebase-v2.ser").toURI());
        when(codeBaseImporter.importPublication(any(CodeBasePublication3.class))).thenThrow(new DuplicateKeyException("Thrown by mock"));

        // when
        boolean handled = publicationImporter.importPublicationFile(file);

        // then
        assertThat(handled, is(false));

        verify(codeBaseImporter).importPublication(any(CodeBasePublication3.class));
        verify(validator).validate(any());
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
    }

    @Test
    public void should_swallow_CodeBasePublication2_when_InvalidClassException() throws URISyntaxException {
        // given
        File file = new File(getClass().getResource("/sample-publications/codebase-v2-bad-serialVersionUID.ser").toURI());

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
        when(invocationDataImporter.importPublication(any(InvocationDataPublication2.class))).thenReturn(true);

        // when
        boolean handled = publicationImporter.importPublicationFile(file);

        // then
        assertThat(handled, is(true));
        verify(invocationDataImporter).importPublication(any(InvocationDataPublication2.class));
        verify(lockManager).acquireLock(LockManager.Lock.IMPORT);
        verify(lockManager).releaseLock(LockManager.Lock.IMPORT);
        verify(validator).validate(any());
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
    }

    @Test
    public void should_reject_InvocationDataPublication2_when_DuplicateKeyException() throws URISyntaxException {
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

        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
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
        when(validator.validate(any())).thenReturn(Collections.singleton(mock(ConstraintViolation.class)));

        // when
        boolean handled = publicationImporter.importPublicationFile(file);

        // then
        assertThat(handled, is(true));
        verify(validator).validate(any());
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator);
    }
}
