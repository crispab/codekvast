package io.codekvast.dashboard.file_import.impl;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.codekvast.common.lock.LockTemplate;
import io.codekvast.dashboard.metrics.AgentMetricsService;
import io.codekvast.dashboard.model.PublicationType;
import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import io.codekvast.javaagent.model.v3.CodeBaseEntry3;
import io.codekvast.javaagent.model.v3.CodeBasePublication3;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** @author olle.hallin@crisp.se */
public class CodeBaseImporterImplTest {

  private static final Instant NOW = Instant.now();

  @Mock private Clock clock;

  @Mock private SyntheticSignatureService syntheticSignatureService;

  @Mock private LockTemplate lockTemplate;

  @Mock private AgentMetricsService metricsService;

  @InjectMocks private CodeBaseImporterImpl codeBaseImporter;

  @BeforeEach
  public void beforeTest() {
    MockitoAnnotations.openMocks(this);
    when(clock.instant()).thenReturn(NOW);
  }

  @Test
  public void should_ignore_synthetic_signatures() throws Exception {
    // given
    String syntheticSignature =
        "customer1.FooConfig..EnhancerBySpringCGLIB..96aac875.CGLIB$BIND_CALLBACKS(java.lang.Object)";
    CodeBasePublication3 publication =
        CodeBasePublication3.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .entries(
                asList(
                    CodeBaseEntry3.sampleCodeBaseEntry(),
                    CodeBaseEntry3.sampleCodeBaseEntry().toBuilder()
                        .signature(syntheticSignature)
                        .build()))
            .build();

    when(syntheticSignatureService.isSyntheticMethod(syntheticSignature)).thenReturn(true);
    when(lockTemplate.doWithLockOrThrow(any(), any())).thenReturn(Duration.ofSeconds(1));

    // when
    codeBaseImporter.importPublication(publication);

    // then
    verify(metricsService)
        .recordImportedPublication(PublicationType.CODEBASE, 1, 1, Duration.ofSeconds(1));
  }
}
