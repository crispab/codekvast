package duck.spike.agent;

import duck.spike.util.SensorUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.net.URISyntaxException;
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
        List<String> signatures = readSignatures(getResource("/signatures-guice-aop.dat"));
        agent.resetSignatureUsage(signatures);
        int unrecognized = agent.applyRecordedUsage(SensorUtils.readUsageFrom(getResource("/usage-guice-aop.dat")));
        assertThat(unrecognized, is(0));
    }

    private File getResource(String resource) throws URISyntaxException {
        return new File(getClass().getResource(resource).toURI());
    }

    private List<String> readSignatures(File file) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
        }
        return result;
    }
}
