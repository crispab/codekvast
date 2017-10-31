package io.codekvast.javaagent.model.v2;

import io.codekvast.javaagent.model.v1.CodeBaseEntry1;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CodeBaseEntry2Test {

    @Test
    public void should_transform_from_v1_format() {
        CodeBaseEntry1 e1 = CodeBaseEntry1.sampleCodeBaseEntry();
        CodeBaseEntry2 e2 = CodeBaseEntry2.sampleCodeBaseEntry();
        assertThat(CodeBaseEntry2.fromV1Format(e1), is(e2));
    }

}