package se.crisp.duck.agent.util;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FileUtilsTest {

    @Test
    public void testReadPropertiesFromClasspath() throws URISyntaxException, IOException {
        URI duck1 = new URI("classpath:/duck1.properties");
        Properties properties = FileUtils.readPropertiesFrom(duck1);
        assertThat(properties.getProperty("appName"), is("appName"));
    }
}
