package se.crisp.codekvast.warehouse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;
import se.crisp.codekvast.warehouse.api.model.ApplicationDescriptor;
import se.crisp.codekvast.warehouse.api.model.EnvironmentDescriptor;
import se.crisp.codekvast.warehouse.api.model.MethodDescriptor;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class MethodDescriptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MethodDescriptor methodDescriptor = MethodDescriptor.builder()
                                                                      .collectedDays(30)
                                                                      .collectedSinceMillis(0L)
                                                                      .declaringType("declaringType")
                                                                      .lastInvokedAtMillis(null)
                                                                      .modifiers("")
                                                                      .occursInApplication(
                                                                              ApplicationDescriptor.builder()
                                                                                                   .name("app1")
                                                                                                   .version("1.2")
                                                                                                   .status(SignatureStatus
                                                                                                                   .EXCLUDED_BY_PACKAGE_NAME)
                                                                                                   .invokedAtMillis(0L)
                                                                                                   .build())
                                                                      .occursInApplication(
                                                                              ApplicationDescriptor.builder()
                                                                                                   .name("app1")
                                                                                                   .version("1.3")
                                                                                                   .status(SignatureStatus.EXACT_MATCH)
                                                                                                   .invokedAtMillis(
                                                                                                           System.currentTimeMillis())
                                                                                                   .build())
                                                                      .occursInApplication(
                                                                              ApplicationDescriptor.builder()
                                                                                                   .name("app1")
                                                                                                   .version("1.2")
                                                                                                   .status(SignatureStatus
                                                                                                                   .EXCLUDED_BY_PACKAGE_NAME)
                                                                                                   .invokedAtMillis(0L)
                                                                                                   .build())
                                                                      .collectedInEnvironment(
                                                                              EnvironmentDescriptor.builder()
                                                                                                   .name("test")
                                                                                                   .collectedSinceMillis(0L)
                                                                                                   .collectedDays(3)
                                                                                                   .build())
                                                                      .collectedInEnvironment(
                                                                              EnvironmentDescriptor.builder()
                                                                                                   .name("training")
                                                                                                   .collectedSinceMillis(
                                                                                                           Instant.now().minus(27, DAYS)
                                                                                                                  .toEpochMilli())
                                                                                                   .collectedDays(27)
                                                                                                   .build())
                                                                      .collectedInEnvironment(
                                                                              EnvironmentDescriptor.builder()
                                                                                                   .name("customer1")
                                                                                                   .collectedSinceMillis(
                                                                                                           Instant.now().minus(33, DAYS)
                                                                                                                  .toEpochMilli())
                                                                                                   .collectedDays(33)
                                                                                                   .build())
                                                                      .packageName("packageName")
                                                                      .signature("signature")
                                                                      .visibility("public")
                                                                      .build();

    @Test
    public void should_serializable_not_invoked_method_to_JSON() throws Exception {
        // given
        MethodDescriptor md = methodDescriptor.toBuilder().lastInvokedAtMillis(null).build();

        // when
        String json = objectMapper.writeValueAsString(md);

        // then
        assertThat(json, containsString("\"lastInvokedAtMillis\":null"));
    }

    @Test
    public void should_serializable_invoked_method_to_JSON() throws Exception {
        // given
        MethodDescriptor md = methodDescriptor.toBuilder().lastInvokedAtMillis(1000L).build();

        // when
        String json = objectMapper.writeValueAsString(md);

        // then
        assertThat(json, containsString("\"lastInvokedAtMillis\":1000"));

        System.out.println("json = " + objectMapper.writer()
                                                   .withDefaultPrettyPrinter()
                                                   .writeValueAsString(md));
    }
}
