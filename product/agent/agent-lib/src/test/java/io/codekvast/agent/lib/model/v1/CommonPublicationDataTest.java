package io.codekvast.agent.lib.model.v1;

import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CommonPublicationDataTest {

    private final CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();

    @Test
    public void should_have_decent_toString() throws Exception {
        CommonPublicationData data = CommonPublicationData.getBuilder(config)
                                                          .codeBaseFingerprint("codeBaseFingerprint")
                                                          .sequenceNumber(3)
                                                          .build();
        assertThat(data.toString(), containsString("publishedAt=2"));
    }
}
