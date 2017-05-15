package io.codekvast.javaagent.util;

import io.codekvast.javaagent.publishing.impl.JulAwareOutputCapture;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import java.net.URISyntaxException;
import java.util.Properties;

import static io.codekvast.javaagent.util.ConfigUtils.*;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConfigUtilsTest {

    private static final String MY_PROP1 = ConfigUtilsTest.class.getName() + ".prop1";
    private static final String MY_PROP2 = ConfigUtilsTest.class.getName() + ".prop2";
    private static final String MY_PROP3 = ConfigUtilsTest.class.getName() + ".prop3";

    @Rule
    public OutputCapture output = new JulAwareOutputCapture();

    @After
    public void afterTest() {
        System.getProperties().remove(MY_PROP1);
        System.getProperties().remove(MY_PROP2);
    }

    @Test
    public void should_compute_codekvast_prefixed_env_var_names() throws Exception {
        assertThat(getEnvVarName("foo"), is("CODEKVAST_FOO"));
        assertThat(getEnvVarName("fooBarBaz"), is("CODEKVAST_FOO_BAR_BAZ"));
    }

    @Test
    public void testGetNormalizedPackagePrefix1() throws URISyntaxException {
        assertThat(getNormalizedPackagePrefix("prefix....."), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix2() throws URISyntaxException {
        assertThat(getNormalizedPackagePrefix("prefix."), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix3() throws URISyntaxException {
        assertThat(getNormalizedPackagePrefix("prefix.foobar..."), is("prefix.foobar"));
    }

    @Test
    public void testGetNormalizedPackagePrefix4() throws URISyntaxException {
        assertThat(getNormalizedPackagePrefix("prefix"), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix5() throws URISyntaxException {
        assertThat(getNormalizedPackagePrefix("p"), is("p"));
    }

    @Test
    public void testGetNormalizedPackagePrefix6() throws URISyntaxException {
        assertThat(getNormalizedPackagePrefix(""), is(""));
    }

    @Test
    public void testGetNormalizedPackages1() throws Exception {
        assertThat(getNormalizedPackages("   com.acme... ; foo.bar..   "),
                   equalTo(asList("com.acme", "foo.bar")));
    }

    @Test
    public void testGetNormalizedPackages2() throws Exception {
        assertThat(getNormalizedPackages(",   , x, : y  ; : com.acme... , foo.bar..  , "),
                   equalTo(asList("com.acme", "foo.bar", "x", "y")));
    }

    @Test
    public void should_expand_variables() {
        System.setProperty(MY_PROP1, "XXX");
        System.setProperty(MY_PROP2, "YYY");
        String userVariableName;
        if (System.getProperty("os.name").startsWith(("Windows"))) {
            userVariableName = "$USERNAME";
        } else {
             userVariableName = "$USER";
        }
        Properties props = new Properties();
        props.setProperty(MY_PROP1, "XXX_from_props");
        props.setProperty(MY_PROP3, "ZZZ");
        assertThat(expandVariables(props, userVariableName + " ${" + MY_PROP1 + "} foo ${" + MY_PROP2 + "} bar ${" + MY_PROP3
                           + "}"),
                   is(System.getProperty("user.name") + " XXX foo YYY bar ZZZ"));

    }

    @Test
    public void should_handle_missing_expansions() {
        assertThat(expandVariables(new Properties(), "foo $missingProp1 bar ${missing.prop2} baz"),
                   is("foo $missingProp1 bar ${missing.prop2} baz"));

        output.expect(containsString("Unrecognized variable: $missingProp1"));
        output.expect(containsString("Unrecognized variable: ${missing.prop2}"));
    }
}
