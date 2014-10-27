package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SharedConfigTest {

    @Test
    public void testGetNormalizedPackagePrefix1() throws URISyntaxException {
        SharedConfig config = getSharedConfig("prefix.....");
        assertThat(config.getNormalizedPackagePrefix(), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix2() throws URISyntaxException {
        SharedConfig config = getSharedConfig("prefix.");
        assertThat(config.getNormalizedPackagePrefix(), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix3() throws URISyntaxException {
        SharedConfig config = getSharedConfig("prefix.foobar...");
        assertThat(config.getNormalizedPackagePrefix(), is("prefix.foobar"));
    }

    @Test
    public void testGetNormalizedPackagePrefix4() throws URISyntaxException {
        SharedConfig config = getSharedConfig("prefix");
        assertThat(config.getNormalizedPackagePrefix(), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix5() throws URISyntaxException {
        SharedConfig config = getSharedConfig("p");
        assertThat(config.getNormalizedPackagePrefix(), is("p"));
    }

    @Test
    public void testGetNormalizedPackagePrefix6() throws URISyntaxException {
        SharedConfig config = getSharedConfig("");
        assertThat(config.getNormalizedPackagePrefix(), is(""));
    }

    private SharedConfig getSharedConfig(String packagePrefix) throws URISyntaxException {
        return SharedConfig.builder()
                           .customerName("customerName")
                           .appName("appName")
                           .dataPath(new File("."))
                           .packagePrefix(packagePrefix).build();
    }
}
