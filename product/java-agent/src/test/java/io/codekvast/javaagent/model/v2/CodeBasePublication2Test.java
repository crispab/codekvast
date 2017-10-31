package io.codekvast.javaagent.model.v2;

import io.codekvast.javaagent.model.v1.CodeBaseEntry1;
import io.codekvast.javaagent.model.v1.CodeBasePublication1;
import io.codekvast.javaagent.model.v1.CommonPublicationData1;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CodeBasePublication2Test {

    @Test
    public void should_transform_from_v1_format() {
        CodeBasePublication1 pub1 = CodeBasePublication1.builder()
                                                        .commonData(CommonPublicationData1.sampleCommonPublicationData())
                                                        .entries(Collections.singletonList(CodeBaseEntry1.sampleCodeBaseEntry()))
                                                        .overriddenSignatures(Collections.singletonMap("key1", "value1"))
                                                        .strangeSignatures(Collections.singletonMap("key2", "value2"))
                                                        .build();
        CodeBasePublication2 pub2 = CodeBasePublication2.fromV1Format(pub1);
        assertThat(pub2.getCommonData(), is(pub1.getCommonData()));
        assertThat(pub2.getEntries(), hasItems(CodeBaseEntry2.sampleCodeBaseEntry()));
    }
}