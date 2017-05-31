package integrationTest.agent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Test that CodeBaseScanner understands Spring Boot's proprietary nested jar format (produced by gradle bootRepackage)
 *
 * @author olle.hallin@crisp.se
 */
@RunWith(MockitoJUnitRunner.class)
public class ScanExecutableSpringBootJarIntegrationTest {
    private final String sampleSpringBootJarDir = System.getProperty("integrationTest.sampleSpringBootJarDir");

    @Test
    public void should_have_directory_containing_boot_repackaged_jar() throws Exception {
        System.out.println("sampleSpringBootJarDir = " + sampleSpringBootJarDir);

        assertThat(sampleSpringBootJarDir, not(nullValue()));
        assertThat(sampleSpringBootJarDir, not(is("")));

        File file = new File(sampleSpringBootJarDir);
        assertThat(file.isDirectory(), is(true));
    }
}
