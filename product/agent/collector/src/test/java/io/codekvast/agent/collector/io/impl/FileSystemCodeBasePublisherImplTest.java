package io.codekvast.agent.collector.io.impl;

import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import io.codekvast.agent.lib.model.v1.CodeBaseEntry;
import io.codekvast.agent.lib.model.v1.CodeBasePublication;
import io.codekvast.agent.lib.model.v1.MethodSignature;
import io.codekvast.agent.lib.model.v1.SignatureStatus;
import io.codekvast.agent.lib.util.Constants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
public class FileSystemCodeBasePublisherImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private CodeBase codeBase;

    private CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();

    private final FileSystemCodeBasePublisherImpl publisher = new FileSystemCodeBasePublisherImpl(config);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void should_expand_hostname_placeholder() throws Exception {
        File file = new File("/tmp/foo-#hostname#.ser");
        File expanded = publisher.expandPlaceholders(file);

        assertThat(expanded.getName(), not(is(file.getName())));
    }

    @Test
    public void should_expand_timestamp_placeholder() throws Exception {
        File file = new File("/tmp/foo-#timestamp#.ser");
        File expanded = publisher.expandPlaceholders(file);

        assertThat(expanded.getName(), not(is(file.getName())));
    }

    @Test
    public void should_recognize_setValue_targetFile() throws Exception {
        boolean recognized = publisher.doSetValue("targetFile", "foobar");
        assertThat(recognized, is(true));
    }

    @Test
    public void should_publish_codebase_to_specified_file() throws Exception {
        // given
        when(codeBase.getCodeBasePublication()).thenReturn(createCodebasePublication());

        File file = new File(temporaryFolder.getRoot(), "codebase.ser");
        assertThat(file.exists(), is(false));

        // when
        publisher.doSetValue("targetFile", file.getAbsolutePath());
        publisher.doPublishCodeBase(codeBase);

        // then
        assertThat(file.exists(), is(true));
    }

    @Test
    public void should_serialize_deserialize_codebase_publication() throws Exception {
        File file = temporaryFolder.newFile();

        CodeBasePublication publication1 = createCodebasePublication();

        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            oos.writeObject(publication1);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            CodeBasePublication publication2 = (CodeBasePublication) ois.readObject();
            assertThat(publication2, is(publication1));
            assertThat(publication1, not(sameInstance(publication2)));
        }
    }

    private CodeBasePublication createCodebasePublication() {
        List<CodeBaseEntry> entries = new ArrayList<>();
        entries.add(new CodeBaseEntry("sig1", MethodSignature.createSampleMethodSignature(), SignatureStatus.NOT_INVOKED));
        entries.add(new CodeBaseEntry("sig2", MethodSignature.createSampleMethodSignature(), SignatureStatus.EXACT_MATCH));

        return CodeBasePublication.builder()
                                  .collectorVersion(Constants.COLLECTOR_VERSION)
                                  .jvmUuid(Constants.JVM_UUID)
                                  .computerId(Constants.COMPUTER_ID)
                                  .publishedAtMillis(System.currentTimeMillis())
                                  .hostName(Constants.HOST_NAME)
                                  .entries(entries)
                                  .appVersion("appVersion")
                                  .appName("appName")
                                  .build();
    }

}