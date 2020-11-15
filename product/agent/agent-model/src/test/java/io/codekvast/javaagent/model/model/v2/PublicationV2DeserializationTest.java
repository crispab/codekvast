package io.codekvast.javaagent.model.model.v2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;

import io.codekvast.javaagent.model.v2.CodeBasePublication2;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.junit.jupiter.api.Test;

/**
 * A test to prove that we can deserialize publications produced by a v2 agent. The test resources
 * are produced by ./gradlew :sample:sample-gradle-application:run
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("deprecation")
public class PublicationV2DeserializationTest {
  private static final String CODEBASE_RESOURCE = "/sample-publications/codebase-v2-0.24.0.ser";
  private static final String INVOCATIONS_RESOURCE =
      "/sample-publications/invocations-v2-0.24.0.ser";

  @Test
  public void should_deserialize_codebaseV2_file() throws IOException, ClassNotFoundException {
    ObjectInputStream ois =
        new ObjectInputStream(
            new BufferedInputStream(getClass().getResourceAsStream(CODEBASE_RESOURCE)));
    CodeBasePublication2 publication = (CodeBasePublication2) ois.readObject();
    assertThat(publication, isA(CodeBasePublication2.class));
  }

  @Test
  public void should_deserialize_invocationsV2_file() throws IOException, ClassNotFoundException {
    ObjectInputStream ois =
        new ObjectInputStream(
            new BufferedInputStream(getClass().getResourceAsStream(INVOCATIONS_RESOURCE)));
    InvocationDataPublication2 publication = (InvocationDataPublication2) ois.readObject();
    assertThat(publication, isA(InvocationDataPublication2.class));
  }
}
