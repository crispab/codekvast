package io.codekvast.agent.lib.model.v1;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CommonPublicationDataTest {

    @Test
    public void should_have_decent_toString() throws Exception {
        CommonPublicationData data = CommonPublicationData.builder()
                                                           .appName("appName")
                                                           .appVersion("appVersion")
                                                           .codeBaseFingerprint("codeBaseFingerprint")
                                                           .collectorVersion("collectorVersion")
                                                           .computerId("computerId")
                                                           .environment("environment")
                                                           .hostName("hostName")
                                                           .jvmUuid("jvmUuid")
                                                           .sequenceNumber(3)
                                                           .publishedAtMillis(System.currentTimeMillis())
                                                           .tags("tags")
                                                           .build();
        assertThat(data.toString(), containsString("publishedAt=2"));
    }
}