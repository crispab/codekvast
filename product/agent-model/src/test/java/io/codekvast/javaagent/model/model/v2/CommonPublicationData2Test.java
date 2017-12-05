package io.codekvast.javaagent.model.model.v2;

import io.codekvast.javaagent.model.v1.CommonPublicationData;
import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CommonPublicationData2Test {

    @SuppressWarnings("deprecation")
    @Test
    public void should_transform_from_v1_format() {
        CommonPublicationData cd1 = CommonPublicationData.sampleCommonPublicationData();
        CommonPublicationData2 cd2 = CommonPublicationData2.sampleCommonPublicationData();
        assertThat(CommonPublicationData2.fromV1format(cd1), is(cd2));
    }
}