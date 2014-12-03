package se.crisp.codekvast.agent.main;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import se.crisp.codekvast.agent.main.support.AppVersionStrategy;
import se.crisp.codekvast.agent.main.support.LiteralAppVersionStrategy;

import static com.google.common.collect.ImmutableList.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AgentWorkerTest {

    private final ImmutableList<? extends AppVersionStrategy> strategies = of(new LiteralAppVersionStrategy());

    @Test
    public void testResolveConstantAppVersion() throws Exception {
        assertThat(AgentWorker.resolveAppVersion(strategies, null, "constant foo"), is("foo"));
    }

    @Test
    public void testResolveLiteralAppVersion() throws Exception {
        assertThat(AgentWorker.resolveAppVersion(strategies, null, "   LITERAL    foo"), is("foo"));
    }

    @Test
    public void testResolveUnrecognizedAppVersion() throws Exception {
        assertThat(AgentWorker.resolveAppVersion(strategies, null, "   FOOBAR    foo   "), is("FOOBAR    foo"));
    }
}
