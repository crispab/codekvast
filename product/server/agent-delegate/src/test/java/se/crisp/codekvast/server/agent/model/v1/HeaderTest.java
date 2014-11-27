package se.crisp.codekvast.server.agent.model.v1;

import org.junit.Test;

public class HeaderTest {

    public static final Header HEADER = Header.builder()
                                              .environment("environment")
                                              .build();

    @Test(expected = NullPointerException.class)
    public void missingValuesShouldBeRejected() {
        Header.builder().build();
    }

}
