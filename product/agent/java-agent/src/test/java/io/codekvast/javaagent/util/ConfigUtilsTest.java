package io.codekvast.javaagent.util;

import io.codekvast.javaagent.publishing.impl.JulAwareOutputCapture;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.system.OutputCaptureRule;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static io.codekvast.javaagent.util.ConfigUtils.*;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConfigUtilsTest {

    private static final String MY_PROP1 = ConfigUtilsTest.class.getName() + ".prop1";
    private static final String MY_PROP2 = ConfigUtilsTest.class.getName() + ".prop2";
    private static final String MY_PROP3 = ConfigUtilsTest.class.getName() + ".prop3";
    private static final String CODEKVAST_APP_VERSION = "codekvast.appVersion";

    @Rule
    public OutputCaptureRule output = new JulAwareOutputCapture();

    @After
    public void afterTest() {
        System.getProperties().remove(MY_PROP1);
        System.getProperties().remove(MY_PROP2);
        System.getProperties().remove(MY_PROP3);
        System.getProperties().remove(CODEKVAST_APP_VERSION);
    }

    @Test
    public void should_compute_codekvast_prefixed_env_var_names() {
        assertThat(getEnvVarName("foo"), is("CODEKVAST_FOO"));
        assertThat(getEnvVarName("fooBarBaz"), is("CODEKVAST_FOO_BAR_BAZ"));
    }

    @Test
    public void should_compute_codekvast_prefixed_system_property_names() {
        assertThat(getSystemPropertyName("fooBar"), is("codekvast.fooBar"));
    }

    @Test
    public void testGetNormalizedPackagePrefix1() {
        assertThat(getNormalizedPackagePrefix("prefix....."), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix2() {
        assertThat(getNormalizedPackagePrefix("prefix."), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix3() {
        assertThat(getNormalizedPackagePrefix("prefix.foobar..."), is("prefix.foobar"));
    }

    @Test
    public void testGetNormalizedPackagePrefix4() {
        assertThat(getNormalizedPackagePrefix("prefix"), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix5() {
        assertThat(getNormalizedPackagePrefix("p"), is("p"));
    }

    @Test
    public void testGetNormalizedPackagePrefix6() {
        assertThat(getNormalizedPackagePrefix(""), is(""));
    }

    @Test
    public void testGetNormalizedPackages1() {
        assertThat(getNormalizedPackages("   com.acme... ; foo.bar..   "),
                   equalTo(asList("com.acme", "foo.bar")));
    }

    @Test
    public void testGetNormalizedPackages2() {
        assertThat(getNormalizedPackages(",   , x, : y  ; : com.acme... , foo.bar..  , "),
                   equalTo(asList("com.acme", "foo.bar", "x", "y")));
    }

    @Test
    public void should_expand_variables() {
        // given
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

        // when
        String actual = expandVariables(props, userVariableName + " ${" + MY_PROP1 + "} foo ${" + MY_PROP2 + "} bar ${" + MY_PROP3 + "}");

        // then
        assertThat(actual, is(System.getProperty("user.name") + " XXX foo YYY bar ZZZ"));
    }

    @Test
    public void should_detect_codekvast_system_properties() {
        System.setProperty(CODEKVAST_APP_VERSION, "sysprop-appVersion");

        Properties props = new Properties();
        props.setProperty("appVersion", "some-app-version");

        assertThat(expandVariables(props, "appVersion", "default-app-version"), is("sysprop-appVersion"));
    }

    @Test
    public void should_expand_null() {
        assertThat(expandVariables(new Properties(), null), nullValue());
    }

    @Test
    public void should_handle_missing_expansions() {
        assertThat(expandVariables(new Properties(), "foo $missingProp1 bar ${missing.prop2} baz"),
                   is("foo $missingProp1 bar ${missing.prop2} baz"));

        output.expect(containsString("Unrecognized variable: $missingProp1"));
        output.expect(containsString("Unrecognized variable: ${missing.prop2}"));
    }

    @Test
    public void should_get_comma_separated_file_values() {
        List<File> files = getCommaSeparatedFileValues("   file1 , file2 ; file3 ");
        assertThat(files, contains(new File("file1"), new File("file2"), new File("file3")));
    }

    @Test
    public void should_getOptionalIntValue_when_present_value() {
        Properties props = new Properties();
        props.setProperty("key", "4711");
        assertThat(getOptionalIntValue(props, "key", 17), is(4711));
    }

    @Test
    public void should_getOptionalIntValue_when_missing_value() {
        assertThat(getOptionalIntValue(new Properties(), "key", 17), is(17));
    }

    @Test
    public void should_getOptionalBooleanValue_when_present_value() {
        Properties props = new Properties();
        props.setProperty("key", "false");
        assertThat(getOptionalBooleanValue(props, "key", true), is(false));
    }

    @Test
    public void should_getOptionalBooleanValue_when_missing_value() {
        assertThat(getOptionalBooleanValue(new Properties(), "key", true), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_when_missing_mandatory_string_value() {
        getMandatoryStringValue(new Properties(), "key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_when_empty_mandatory_string_value() {
        Properties props = new Properties();
        props.setProperty("key", "");

        getMandatoryStringValue(props, "key");
    }

    @Test
    public void should_get_mandatory_string_value() {
        Properties props = new Properties();
        props.setProperty("key", "value");

        assertThat(getMandatoryStringValue(props, "key"), is("value"));
    }
}
