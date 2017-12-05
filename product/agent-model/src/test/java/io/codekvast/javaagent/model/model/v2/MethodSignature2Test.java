package io.codekvast.javaagent.model.model.v2;

import io.codekvast.javaagent.model.v1.MethodSignature;
import io.codekvast.javaagent.model.v2.MethodSignature2;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class MethodSignature2Test {

    @SuppressWarnings("deprecation")
    @Test
    public void should_transform_from_v1_format() {
        MethodSignature m1 = MethodSignature.createSampleMethodSignature();
        MethodSignature2 m2 = MethodSignature2.createSampleMethodSignature();
        assertThat(MethodSignature2.fromV1Format(m1), is(m2));
    }
}