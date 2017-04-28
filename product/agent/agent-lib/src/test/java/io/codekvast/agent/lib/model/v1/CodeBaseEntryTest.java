package io.codekvast.agent.lib.model.v1;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author olle.hallin@crisp.se
 */
public class CodeBaseEntryTest {

    @Test
    public void should_split_normalized_signature() throws Exception {
        CodeBaseEntry entry =
            new CodeBaseEntry("public void foo()", MethodSignature.createSampleMethodSignature(), SignatureStatus.NOT_INVOKED);

        assertThat(entry.getVisibility(), is("public"));
        assertThat(entry.getSignature(), is("void foo()"));
    }
}