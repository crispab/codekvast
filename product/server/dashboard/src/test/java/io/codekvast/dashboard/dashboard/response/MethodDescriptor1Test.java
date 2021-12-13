package io.codekvast.dashboard.dashboard.response;

import static io.codekvast.javaagent.model.v2.SignatureStatus2.EXCLUDED_BY_PACKAGE_NAME;
import static io.codekvast.javaagent.model.v2.SignatureStatus2.INVOKED;
import static io.codekvast.javaagent.model.v2.SignatureStatus2.NOT_INVOKED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codekvast.dashboard.dashboard.model.methods.ApplicationDescriptor;
import io.codekvast.dashboard.dashboard.model.methods.EnvironmentDescriptor;
import io.codekvast.dashboard.dashboard.model.methods.MethodDescriptor1;
import org.junit.jupiter.api.Test;

/** @author olle.hallin@crisp.se */
public class MethodDescriptor1Test {

  private final long days = 24 * 60 * 60 * 1000L;
  private final long now = System.currentTimeMillis();

  private final long twoDaysAgo = now - 2 * days;
  private final long fourteenDaysAgo = now - 14 * days;
  private final long never = 0L;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void should_calculate_min_max_correctly() {
    // given
    MethodDescriptor1 md = buildMethodDescriptor(fourteenDaysAgo, twoDaysAgo, never, twoDaysAgo);

    // when

    // then
    assertThat(toDaysAgo(md.getCollectedSinceMillis()), is(toDaysAgo(fourteenDaysAgo)));
    assertThat(toDaysAgo(md.getCollectedToMillis()), is(toDaysAgo(twoDaysAgo)));
    assertThat(md.getCollectedDays(), is(12));
    assertThat(toDaysAgo(md.getLastInvokedAtMillis()), is(toDaysAgo(twoDaysAgo)));

    assertThat(md.getTrackedPercent(), is(67));
    assertThat(
        md.getStatuses(), containsInAnyOrder(INVOKED, NOT_INVOKED, EXCLUDED_BY_PACKAGE_NAME));
  }

  private int toDaysAgo(long timestamp) {
    return Math.toIntExact((now - timestamp) / days);
  }

  @Test
  public void should_serializable_to_JSON() throws Exception {
    // given
    MethodDescriptor1 md = buildMethodDescriptor(fourteenDaysAgo, twoDaysAgo, never, twoDaysAgo);
    long lastInvokedAtMillis = md.getLastInvokedAtMillis();

    // when
    String json = objectMapper.writeValueAsString(md);

    // then
    assertThat(json, containsString("\"lastInvokedAtMillis\":" + lastInvokedAtMillis));

    System.out.println(
        "json = " + objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(md));
  }

  private MethodDescriptor1 buildMethodDescriptor(
      long collectedSinceMillis,
      long collectedToMillis,
      long invokedAtMillis1,
      long invokedAtMillis2) {
    return MethodDescriptor1.builder()
        .id(1L)
        .declaringType("declaringType")
        .modifiers("")
        .occursInApplication(
            ApplicationDescriptor.builder()
                .name("app1")
                .status(EXCLUDED_BY_PACKAGE_NAME)
                .collectedSinceMillis(collectedSinceMillis)
                .collectedToMillis(collectedToMillis)
                .invokedAtMillis(invokedAtMillis1)
                .build())
        .occursInApplication(
            ApplicationDescriptor.builder()
                .name("app2")
                .status(NOT_INVOKED)
                .collectedSinceMillis(collectedSinceMillis + 10)
                .collectedToMillis(collectedToMillis - 10)
                .invokedAtMillis(invokedAtMillis1 - 10)
                .build())
        .occursInApplication(
            ApplicationDescriptor.builder()
                .name("app3")
                .status(INVOKED)
                .collectedSinceMillis(collectedSinceMillis)
                .collectedToMillis(collectedToMillis)
                .invokedAtMillis(invokedAtMillis2)
                .build())
        .collectedInEnvironment(
            EnvironmentDescriptor.builder()
                .name("test")
                .collectedSinceMillis(collectedSinceMillis)
                .collectedToMillis(collectedToMillis)
                .invokedAtMillis(invokedAtMillis2)
                .build()
                .computeFields())
        .collectedInEnvironment(
            EnvironmentDescriptor.builder()
                .name("customer1")
                .collectedSinceMillis(collectedSinceMillis)
                .collectedToMillis(collectedToMillis)
                .invokedAtMillis(invokedAtMillis2)
                .build()
                .computeFields())
        .packageName("packageName")
        .signature("signature")
        .visibility("public")
        .bridge(null)
        .synthetic(false)
        .location("location1")
        .location("location2")
        .build()
        .computeFields();
  }
}
