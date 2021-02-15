package io.codekvast.intake.file_import.impl

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.codekvast.common.lock.LockTemplate
import io.codekvast.common.messaging.EventService
import io.codekvast.intake.metrics.IntakeMetricsService
import io.codekvast.intake.model.PublicationType
import io.codekvast.intake.model.PublicationType.CODEBASE
import io.codekvast.javaagent.model.v2.CommonPublicationData2
import io.codekvast.javaagent.model.v3.CodeBaseEntry3
import io.codekvast.javaagent.model.v3.CodeBasePublication3
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.*
import org.mockito.Mockito.verify
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable

/** @author olle.hallin@crisp.se
 */
class CodeBaseImporterImplTest {
    @Mock
    private lateinit var clock: Clock

    @Mock
    private lateinit var syntheticSignatureService: SyntheticSignatureService

    @Mock
    private lateinit var lockTemplate: LockTemplate

    @Mock
    private lateinit var metricsService: IntakeMetricsService

    @Mock
    private lateinit var commonImporter: CommonImporter

    @Mock
    private lateinit var importDAO: ImportDAO

    @Mock
    private lateinit var eventService: EventService

    @InjectMocks
    private lateinit var codeBaseImporter: CodeBaseImporterImpl

    @BeforeEach
    fun beforeTest() {
        MockitoAnnotations.openMocks(this)
        whenever(clock.instant()).thenReturn(Instant.now())
    }

    @Test
    fun should_ignore_synthetic_signatures() {
        // given
        val syntheticSignature =
            "customer1.FooConfig..EnhancerBySpringCGLIB..96aac875.CGLIB\$BIND_CALLBACKS(java.lang.Object)"
        val publication = CodeBasePublication3.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .entries(
                listOf(
                    CodeBaseEntry3.sampleCodeBaseEntry(),
                    CodeBaseEntry3.sampleCodeBaseEntry().toBuilder()
                        .signature(syntheticSignature)
                        .build()
                )
            )
            .build()
        whenever(syntheticSignatureService.isSyntheticMethod(syntheticSignature)).thenReturn(true)
        whenever(lockTemplate.doWithLockOrThrow(any(), any<Callable<Any>>()))
            .thenReturn(Duration.ofSeconds(1))

        // when
        codeBaseImporter.importPublication(publication)

        // then
        verify(metricsService).recordImportedPublication(CODEBASE, 1, 1, Duration.ofSeconds(1))
    }
}