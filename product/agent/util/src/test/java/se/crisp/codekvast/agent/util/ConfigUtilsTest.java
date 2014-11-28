package se.crisp.codekvast.agent.util;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class ConfigUtilsTest {

    @Test
    public void testGetNormalizedPackagePrefix1() throws URISyntaxException {
        assertThat(ConfigUtils.getNormalizedPackagePrefix("prefix....."), CoreMatchers.is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix2() throws URISyntaxException {
        assertThat(ConfigUtils.getNormalizedPackagePrefix("prefix."), CoreMatchers.is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix3() throws URISyntaxException {
        assertThat(ConfigUtils.getNormalizedPackagePrefix("prefix.foobar..."), CoreMatchers.is("prefix.foobar"));
    }

    @Test
    public void testGetNormalizedPackagePrefix4() throws URISyntaxException {
        assertThat(ConfigUtils.getNormalizedPackagePrefix("prefix"), CoreMatchers.is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix5() throws URISyntaxException {
        assertThat(ConfigUtils.getNormalizedPackagePrefix("p"), CoreMatchers.is("p"));
    }

    @Test
    public void testGetNormalizedPackagePrefix6() throws URISyntaxException {
        assertThat(ConfigUtils.getNormalizedPackagePrefix(""), CoreMatchers.is(""));
    }

    @Test
    public void testGetNormalizedPrefixes1() throws Exception {
        assertThat(ConfigUtils.getNormalizedPackagePrefixes("   com.acme... ; foo.bar..   "), hasItems("com.acme", "foo.bar"));
    }

    @Test
    public void testGetNormalizedPrefixes2() throws Exception {
        assertThat(ConfigUtils.getNormalizedPackagePrefixes(",   , x, : y  ; : com.acme... , foo.bar..  , "),
                   hasItems("x", "y", "com.acme", "foo.bar"));
    }
}
