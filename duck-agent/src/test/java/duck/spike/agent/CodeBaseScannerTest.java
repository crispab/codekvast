package duck.spike.agent;

import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CodeBaseScannerTest {

    private final CodeBaseScanner scanner = new CodeBaseScanner();

    @Test(expected = NullPointerException.class)
    public void testGetUrlsForNullFile() throws Exception {
        scanner.getUrlsForCodeBase(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUrlsForMissingFile() throws Exception {
        scanner.getUrlsForCodeBase(new File("   "));
    }

    @Test
    public void testGetUrlsForDirectoryWithoutJars() throws MalformedURLException {
        URL[] urls = scanner.getUrlsForCodeBase(new File("build/classes/main"));
        assertThat(urls, notNullValue());
        assertThat(urls.length, is(1));
    }

    @Test
    public void testGetUrlsForDirectoryContainingJars() throws MalformedURLException {
        URL[] urls = scanner.getUrlsForCodeBase(new File("src/test/resources/lib1"));
        assertThat(urls, notNullValue());
        assertThat(urls.length, is(3));
    }
}
