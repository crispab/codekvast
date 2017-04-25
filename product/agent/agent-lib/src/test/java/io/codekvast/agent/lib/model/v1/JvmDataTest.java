package io.codekvast.agent.lib.model.v1;

import io.codekvast.agent.lib.model.v1.legacy.JvmData;
import org.junit.Test;

/**
 * @author olle.hallin@crisp.se
 */
public class JvmDataTest {
    @Test
    public void should_create_sample() throws Exception {
        // given

        // when
        JvmData jvmData = JvmData.createSampleJvmData();

        // then
        // should not crash on missing non-null values
    }
}
