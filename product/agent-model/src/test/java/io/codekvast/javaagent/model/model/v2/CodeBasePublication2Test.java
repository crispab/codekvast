package io.codekvast.javaagent.model.model.v2;

import io.codekvast.javaagent.model.v1.CodeBaseEntry;
import io.codekvast.javaagent.model.v1.CodeBasePublication;
import io.codekvast.javaagent.model.v1.CommonPublicationData;
import io.codekvast.javaagent.model.v2.CodeBaseEntry2;
import io.codekvast.javaagent.model.v2.CodeBasePublication2;
import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CodeBasePublication2Test {

    @SuppressWarnings("deprecation")
    @Test
    public void should_transform_from_v1_format() {
        CodeBasePublication pub1 = CodeBasePublication.builder()
                                                      .commonData(CommonPublicationData.sampleCommonPublicationData())
                                                      .entries(Collections.singletonList(CodeBaseEntry.sampleCodeBaseEntry()))
                                                      .overriddenSignatures(Collections.singletonMap("key1", "value1"))
                                                      .strangeSignatures(Collections.singletonMap("key2", "value2"))
                                                      .build();
        CodeBasePublication2 pub2 = CodeBasePublication2.fromV1Format(pub1);
        assertThat(pub2.getCommonData(), is(CommonPublicationData2.sampleCommonPublicationData()));
        assertThat(pub2.getEntries(), hasItems(CodeBaseEntry2.sampleCodeBaseEntry()));
    }
}