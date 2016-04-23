package se.crisp.codekvast.warehouse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;
import se.crisp.codekvast.warehouse.api.model.ApplicationDescriptor;
import se.crisp.codekvast.warehouse.api.model.EnvironmentDescriptor;
import se.crisp.codekvast.warehouse.api.model.MethodDescriptor;

import java.util.Comparator;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class MethodDescriptorTest {

    private final long days = 24 * 60 * 60 * 1000L;
    private final long now = System.currentTimeMillis();

    private final long oneDayAgo = now - 1 * days;
    private final long twoDaysAgo = now - 2 * days;
    private final long fourteenDaysAgo = now - 14 * days;
    private final long fifteenDaysAgo = now - 15 * days;
    private final long never = 0L;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void should_calculate_min_max_correctly() throws Exception {
        // given
        MethodDescriptor md = buildMethodDescriptor(1L, "signature", fourteenDaysAgo, twoDaysAgo, never, twoDaysAgo);

        // when

        // then
        assertThat(toDaysAgo(md.getCollectedSinceMillis()), is(toDaysAgo(fourteenDaysAgo)));
        assertThat(toDaysAgo(md.getCollectedToMillis()), is(toDaysAgo(twoDaysAgo)));
        assertThat(md.getCollectedDays(), is(12));
        assertThat(toDaysAgo(md.getLastInvokedAtMillis()), is(toDaysAgo(twoDaysAgo)));
    }

    int toDaysAgo(long timestamp) {
        return Math.toIntExact((now - timestamp) / days);
    }

    @Test
    public void should_serializable_to_JSON() throws Exception {
        // given
        MethodDescriptor md = buildMethodDescriptor(1L, "signature", fourteenDaysAgo, twoDaysAgo, never, twoDaysAgo);
        long lastInvokedAtMillis = md.getLastInvokedAtMillis();

        // when
        String json = objectMapper.writeValueAsString(md);

        // then
        assertThat(json, containsString("\"lastInvokedAtMillis\":" + lastInvokedAtMillis));

        System.out.println("json = " + objectMapper.writer()
                                                   .withDefaultPrettyPrinter()
                                                   .writeValueAsString(md));
    }

    @Test
    public void should_provide_comparator_for_all_orderBy_values() throws Exception {
        for (QueryMethodsBySignatureParameters.OrderBy orderBy : QueryMethodsBySignatureParameters.OrderBy.values()) {
            assertThat(MethodDescriptor.getComparator(orderBy), not(nullValue()));
        }
    }

    @Test
    public void should_compare_by_signature() throws Exception {
        // given
        MethodDescriptor md1 = buildMethodDescriptor(1L, "sig1", fifteenDaysAgo, oneDayAgo, twoDaysAgo, never);
        MethodDescriptor md2 = buildMethodDescriptor(2L, "sig2", fifteenDaysAgo, oneDayAgo, twoDaysAgo, never);
        MethodDescriptor md3 = buildMethodDescriptor(2L, "sig2", fifteenDaysAgo, oneDayAgo, twoDaysAgo, never);

        // when
        Comparator<MethodDescriptor> comparator = new MethodDescriptor.BySignatureComparator();

        // then
        assertThat(comparator.compare(md1, md2), is(-1));
        assertThat(comparator.compare(md2, md1), is(1));
        assertThat(comparator.compare(md2, md3), is(0));
    }

    @Test
    public void should_compare_by_last_invoked_at_millis() throws Exception {
        // given
        MethodDescriptor md1 = buildMethodDescriptor(1L, "sig1", fifteenDaysAgo, oneDayAgo, twoDaysAgo, never);
        MethodDescriptor md2 = buildMethodDescriptor(2L, "sig2", fifteenDaysAgo, oneDayAgo, oneDayAgo, never);
        MethodDescriptor md3 = buildMethodDescriptor(2L, "sig2", fifteenDaysAgo, oneDayAgo, oneDayAgo, never);

        // when
        Comparator<MethodDescriptor> comparatorAsc = new MethodDescriptor.ByInvokedAtComparatorAsc();
        Comparator<MethodDescriptor> comparatorDesc = new MethodDescriptor.ByInvokedAtComparatorDesc();

        // then
        assertThat(comparatorAsc.compare(md1, md2), is(-1));
        assertThat(comparatorAsc.compare(md2, md1), is(1));
        assertThat(comparatorAsc.compare(md2, md3), is(0));

        assertThat(comparatorDesc.compare(md1, md2), is(1));
        assertThat(comparatorDesc.compare(md2, md1), is(-1));
        assertThat(comparatorDesc.compare(md2, md3), is(0));
    }

    private MethodDescriptor buildMethodDescriptor(long methodId, String signature, long collectedSinceMillis, long collectedToMillis,
                                                   long invokedAtMillis1, long invokedAtMillis2) {
        return MethodDescriptor.builder()
                               .id(methodId)
                               .declaringType("declaringType")
                               .modifiers("")
                               .occursInApplication(
                                       ApplicationDescriptor.builder()
                                                            .name("app1")
                                                            .version("1.2")
                                                            .status(SignatureStatus
                                                                            .EXCLUDED_BY_PACKAGE_NAME)
                                                            .startedAtMillis(collectedSinceMillis)
                                                            .dumpedAtMillis(collectedToMillis)
                                                            .invokedAtMillis(invokedAtMillis1)
                                                            .build())
                               .occursInApplication(
                                       ApplicationDescriptor.builder()
                                                            .name("app1")
                                                            .version("1.3")
                                                            .status(SignatureStatus.EXACT_MATCH)
                                                            .startedAtMillis(collectedSinceMillis)
                                                            .dumpedAtMillis(collectedToMillis)
                                                            .invokedAtMillis(invokedAtMillis2)
                                                            .build())
                               .collectedInEnvironment(
                                       EnvironmentDescriptor.builder()
                                                            .name("test")
                                                            .collectedSinceMillis(collectedSinceMillis)
                                                            .collectedToMillis(collectedToMillis)
                                                            .invokedAtMillis(invokedAtMillis2)
                                                            .tag("tag2=2")
                                                            .tag("tag1=1")
                                                            .build())
                               .collectedInEnvironment(
                                       EnvironmentDescriptor.builder()
                                                            .name("customer1")
                                                            .collectedSinceMillis(collectedSinceMillis)
                                                            .collectedToMillis(collectedToMillis)
                                                            .invokedAtMillis(invokedAtMillis2)
                                                            .hostName("server1.customer1.com")
                                                            .hostName("server2.customer1.com")
                                                            .tag("foo=1")
                                                            .tag("bar=2")
                                                            .tag("baz")
                                                            .build())
                               .packageName("packageName")
                               .signature(signature)
                               .visibility("public")
                               .build();
    }

}
