package io.codekvast.javaagent.model.model.v1;

import io.codekvast.javaagent.model.v1.CodeBasePublication;
import io.codekvast.javaagent.model.v1.InvocationDataPublication;
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
public class PublicationV1DeserializationTest {
    private static final String CODEBASE_RESOURCE = "/sample-publications/codebase-v1.ser";
    private static final String INVOCATIONS_RESOURCE = "/sample-publications/invocations-v1.ser";

    @Test
    public void should_deserialize_codebaseV1_file() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(getClass().getResourceAsStream(CODEBASE_RESOURCE)));
        CodeBasePublication publication = (CodeBasePublication) ois.readObject();
        assertThat(publication, isA(CodeBasePublication.class));
    }

    @Test
    public void should_deserialize_invocationsV1_file() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(getClass().getResourceAsStream(INVOCATIONS_RESOURCE)));
        InvocationDataPublication publication = (InvocationDataPublication) ois.readObject();
        assertThat(publication, isA(InvocationDataPublication.class));
    }
}
