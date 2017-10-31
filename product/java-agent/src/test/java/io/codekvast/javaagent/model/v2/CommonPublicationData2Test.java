package io.codekvast.javaagent.model.v2;

import io.codekvast.javaagent.model.v1.CommonPublicationData1;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CommonPublicationData2Test {

    @Test
    public void should_transform_from_v1_format() {
        CommonPublicationData1 cd1 = CommonPublicationData1.sampleCommonPublicationData();
        CommonPublicationData2 cd2 = CommonPublicationData2.fromV1format(cd1);
        assertThat(cd2, is(CommonPublicationData2.sampleCommonPublicationData()));
    }
}