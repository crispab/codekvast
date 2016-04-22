package se.crisp.codekvast.warehouse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;
import se.crisp.codekvast.warehouse.api.model.ApplicationDescriptor;
import se.crisp.codekvast.warehouse.api.model.ApplicationId;
import se.crisp.codekvast.warehouse.api.model.EnvironmentDescriptor;
import se.crisp.codekvast.warehouse.api.model.MethodDescriptor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class MethodDescriptorTest {

    private final long days = 24 * 60 * 60 * 1000L;
    private final long now = System.currentTimeMillis();

    private final long oneDayAgo = now - 1 * days;
    private final long twoDaysAgo = now - 2 * days;
    private final long twelveDaysAgo = now - 12 * days;
    private final long fourteenDaysAgo = now - 14 * days;
    private final long fifteenDaysAgo = now - 15 * days;
    private final long never = 0L;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MethodDescriptor methodDescriptor = MethodDescriptor.builder()
                                                                      .id(1L)
                                                                      .declaringType("declaringType")
                                                                      .modifiers("")
                                                                      .occursInApplication(
                                                                              ApplicationId.of("app1", "1.3"),
                                                                              ApplicationDescriptor.builder()
                                                                                                   .status(SignatureStatus.EXACT_MATCH)
                                                                                                   .startedAtMillis(twelveDaysAgo)
                                                                                                   .dumpedAtMillis(oneDayAgo)
                                                                                                   .invokedAtMillis(twoDaysAgo)
                                                                                                   .build())
                                                                      .occursInApplication(
                                                                              ApplicationId.of("app1", "1.2"),
                                                                              ApplicationDescriptor.builder()
                                                                                                   .status(SignatureStatus
                                                                                                                   .EXCLUDED_BY_PACKAGE_NAME)
                                                                                                   .startedAtMillis(fourteenDaysAgo)
                                                                                                   .dumpedAtMillis(twelveDaysAgo)
                                                                                                   .invokedAtMillis(never)
                                                                                                   .build())
                                                                      .occursInApplication(
                                                                              ApplicationId.of("app1", "1.2"),
                                                                              ApplicationDescriptor.builder()
                                                                                                   .status(SignatureStatus
                                                                                                                   .EXCLUDED_BY_PACKAGE_NAME)
                                                                                                   .startedAtMillis(fifteenDaysAgo)
                                                                                                   .dumpedAtMillis(twelveDaysAgo)
                                                                                                   .invokedAtMillis(never)
                                                                                                   .build())
                                                                      .collectedInEnvironment(
                                                                              "test",
                                                                              EnvironmentDescriptor.builder()
                                                                                                   .collectedSinceMillis(twelveDaysAgo)
                                                                                                   .collectedToMillis(oneDayAgo)
                                                                                                   .invokedAtMillis(twoDaysAgo)
                                                                                                   .build())
                                                                      .collectedInEnvironment(
                                                                              "training",
                                                                              EnvironmentDescriptor.builder()
                                                                                                   .collectedSinceMillis(fifteenDaysAgo)
                                                                                                   .collectedToMillis(oneDayAgo)
                                                                                                   .invokedAtMillis(twoDaysAgo)
                                                                                                   .build())
                                                                      .collectedInEnvironment(
                                                                              "customer1",
                                                                              EnvironmentDescriptor.builder()
                                                                                                   .collectedSinceMillis(twelveDaysAgo)
                                                                                                   .collectedToMillis(twoDaysAgo)
                                                                                                   .invokedAtMillis(twoDaysAgo)
                                                                                                   .hostName("server1.customer1.com")
                                                                                                   .hostName("server2.customer1.com")
                                                                                                   .tag("foo=1")
                                                                                                   .tag("bar=2")
                                                                                                   .tag("baz")
                                                                                                   .build())
                                                                      .packageName("packageName")
                                                                      .signature("signature")
                                                                      .visibility("public")
                                                                      .build();

    @Test
    public void should_calculate_min_max_correctly() throws Exception {
        // given

        // when

        // then
        assertThat(toDaysAgo(methodDescriptor.getCollectedSinceMillis()), is(toDaysAgo(fifteenDaysAgo)));
        assertThat(toDaysAgo(methodDescriptor.getCollectedToMillis()), is(toDaysAgo(oneDayAgo)));
        assertThat(methodDescriptor.getCollectedDays(), is(14));
        assertThat(toDaysAgo(methodDescriptor.getLastInvokedAtMillis()), is(toDaysAgo(twoDaysAgo)));
    }

    int toDaysAgo(long timestamp) {
        return Math.toIntExact((now - timestamp) / days);
    }

    @Test
    public void should_serializable_to_JSON() throws Exception {
        // given
        long lastInvokedAtMillis = methodDescriptor.getLastInvokedAtMillis();

        // when
        String json = objectMapper.writeValueAsString(methodDescriptor);

        // then
        assertThat(json, containsString("\"lastInvokedAtMillis\":" + lastInvokedAtMillis));

        System.out.println("json = " + objectMapper.writer()
                                                   .withDefaultPrettyPrinter()
                                                   .writeValueAsString(methodDescriptor));
    }
}
