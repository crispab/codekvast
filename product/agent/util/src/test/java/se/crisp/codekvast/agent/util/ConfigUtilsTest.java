package se.crisp.codekvast.agent.util;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConfigUtilsTest {

    private static final String MY_PROP1 = ConfigUtilsTest.class.getName() + ".prop1";
    private static final String MY_PROP2 = ConfigUtilsTest.class.getName() + ".prop2";

    @After
    public void afterTest() {
        System.getProperties().remove(MY_PROP1);
        System.getProperties().remove(MY_PROP2);
    }

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

    @Test
    public void testExpandExistingVariables() {
        System.setProperty(MY_PROP1, "XXX");
        System.setProperty(MY_PROP2, "YYY");
        String userVariableName;
        if (System.getProperty("os.name").startsWith(("Windows"))) {
            userVariableName = "$USERNAME";
        } else {
             userVariableName = "$USER";
        }
        assertThat(ConfigUtils.expandVariables(userVariableName + " ${" + MY_PROP1 + "} foo ${" + MY_PROP2 + "} bar"),
                   is(System.getProperty("user.name") + " XXX foo YYY bar"));

    }

    @Test
    public void testExpandMissingVariable() {
        String nonExistingEnvVar = "MYVAR_" + UUID.randomUUID().toString().replaceAll("[-_]", "").toUpperCase();

        assertThat(ConfigUtils.expandVariables("$" + nonExistingEnvVar + " ${missing.prop1} foo ${missing.prop2} bar"),
                   is("$" + nonExistingEnvVar + " ${missing.prop1} foo ${missing.prop2} bar"));
    }
}
