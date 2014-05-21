package duck.spike.agent;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AgentTest {

    @InjectMocks
    private Agent agent;

    @Test
    @Ignore("Broken until signature normalization is implemented")
    public void testPrepareCodeBase() throws Exception {
        List<String> signatures = getSignaturesFromResource("/signatures-guice-aop.dat");
        agent.prepareCodeBase(signatures);
        int unrecognized = agent.applyLatestSensorDataToCodeBase(getResourceFile("/usage-guice-aop.dat"));
        assertThat(unrecognized, is(0));
    }

    private File getResourceFile(String resource) throws URISyntaxException {
        URL url = getClass().getResource(resource);
        return new File(url.toURI());
    }

    private List<String> getSignaturesFromResource(String resource) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream(resource)))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
        }
        return result;
    }
}
