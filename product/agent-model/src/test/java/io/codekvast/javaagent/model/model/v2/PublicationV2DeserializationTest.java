package io.codekvast.javaagent.model.model.v2;

import io.codekvast.javaagent.model.v2.CodeBasePublication2;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertThat;

/**
 * A test to prove that we can deserialize publications produced by a pre-v2 agent.
 * The test resources are produced by gradle :sample:sample-gradle-application:run
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("deprecation")
public class PublicationV2DeserializationTest {
    private static final String CODEBASE_RESOURCE = "/sample-publications/codebase-v2.ser";
    private static final String INVOCATIONS_RESOURCE = "/sample-publications/invocations-v2.ser";

    @Test
    public void should_deserialize_codebaseV2_file() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(getClass().getResourceAsStream(CODEBASE_RESOURCE)));
        CodeBasePublication2 publication = (CodeBasePublication2) ois.readObject();
        assertThat(publication, isA(CodeBasePublication2.class));
    }

    @Test
    public void should_deserialize_invocationsV2_file() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(getClass().getResourceAsStream(INVOCATIONS_RESOURCE)));
        InvocationDataPublication2 publication = (InvocationDataPublication2) ois.readObject();
        assertThat(publication, isA(InvocationDataPublication2.class));
    }
}
