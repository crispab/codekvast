package se.crisp.codekvast.agent.main;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import se.crisp.codekvast.agent.main.appversion.AppVersionStrategy;
import se.crisp.codekvast.agent.main.appversion.LiteralAppVersionStrategy;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AgentWorkerTest {

    private final Collection<AppVersionStrategy> appVersionStrategies = new ArrayList<>();

    @Before
    public void before() throws Exception {
        appVersionStrategies.add(new LiteralAppVersionStrategy());
    }

    @Test
    public void testResolveConstantAppVersion() throws Exception {
        assertThat(AgentWorker.resolveAppVersion(appVersionStrategies, null, "constant foo"), is("foo"));
    }

    @Test
    public void testResolveLiteralAppVersion() throws Exception {
        assertThat(AgentWorker.resolveAppVersion(appVersionStrategies, null, "   LITERAL    foo"), is("foo"));
    }

    @Test
    public void testResolveUnrecognizedAppVersion() throws Exception {
        assertThat(AgentWorker.resolveAppVersion(appVersionStrategies, null, "   FOOBAR    foo   "), is("FOOBAR    foo"));
    }

}
