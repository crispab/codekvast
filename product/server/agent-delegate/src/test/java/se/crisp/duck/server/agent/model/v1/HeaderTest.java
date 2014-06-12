package se.crisp.duck.server.agent.model.v1;

import org.junit.Test;

public class HeaderTest {

    public static final Header HEADER = Header.builder()
                                              .customerName("customerName")
                                              .appName("appName")
                                              .appVersion("appVersion")
                                              .environment("environment")
                                              .codeBaseName("codeBaseName").build();

    @Test(expected = NullPointerException.class)
    public void missingValuesShouldBeRejected() {
        Header.builder().build();
    }

}
