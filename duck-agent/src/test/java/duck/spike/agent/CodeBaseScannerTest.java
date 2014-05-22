package duck.spike.agent;

import duck.spike.util.Configuration;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class CodeBaseScannerTest {

    public static final String SAMPLE_APP_LIB = "src/test/resources/sample-app/lib";
    public static final String SAMPLE_APP_JAR = SAMPLE_APP_LIB + "/sample-app.jar";

    private final CodeBaseScanner scanner = new CodeBaseScanner();
    private final Map<String, String> overriddenMethods = new HashMap<>();
    private final String packagePrefix = getClass().getPackage().getName().substring(0, 3);

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
        URL[] urls = scanner.getUrlsForCodeBase(new File(SAMPLE_APP_LIB));
        assertThat(urls, notNullValue());
        assertThat(urls.length, is(3));
    }

    @Test
    public void testGetUrlsForSingleJar() throws MalformedURLException {
        URL[] urls = scanner.getUrlsForCodeBase(new File(SAMPLE_APP_JAR));
        assertThat(urls, notNullValue());
        assertThat(urls.length, is(1));
    }

    @Test
    public void testScanCodeBaseForSingleJar() throws URISyntaxException {
        Configuration config = Configuration.builder()
                                            .packagePrefix("se.crisp")
                                            .codeBaseUri(new File(SAMPLE_APP_JAR).toURI())
                                            .build();
        CodeBaseScanner.Result result = scanner.getPublicMethodSignatures(config);
        assertThat(result.signatures, notNullValue());
        assertThat(result.signatures.size(), is(21));
    }

    @Test
    public void testScanCodeBaseForDirectoryContainingMultipleJars() throws URISyntaxException {
        Configuration config = Configuration.builder()
                                            .packagePrefix("se.crisp")
                                            .codeBaseUri(new File(SAMPLE_APP_LIB).toURI())
                                            .build();
        CodeBaseScanner.Result result = scanner.getPublicMethodSignatures(config);
        assertThat(result.signatures, notNullValue());
        assertThat(result.signatures.size(), is(21));
    }

    @Test
    public void testFindParentMethod() throws NoSuchMethodException {
        Method method = SubSubClass.class.getMethod("foo", new Class[]{});

        scanner.findParentMethods("xxx", method, SubSubClass.class.getSuperclass(), overriddenMethods, packagePrefix);

        assertThat(overriddenMethods.size(), is(1));
        assertThat(overriddenMethods.get("xxx"), containsString("SubClass.foo()"));
    }

    @Test
    public void testFindGrandParentMethod() throws NoSuchMethodException {
        Method method = SubSubClass.class.getMethod("bar", new Class[]{});

        scanner.findParentMethods("xxx", method, SubSubClass.class.getSuperclass(), overriddenMethods, packagePrefix);

        assertThat(overriddenMethods.size(), is(1));
        assertThat(overriddenMethods.get("xxx"), containsString("BaseClass.bar()"));
    }

    @SuppressWarnings("UnusedDeclaration")
    static class BaseClass {
        public String foo() {
            return "BaseClass.foo()";
        }

        public String bar() {
            return "BaseClass.bar()";
        }
    }

    static class SubClass extends BaseClass {
        @Override
        public String foo() {
            return "SubClass.foo()";
        }

        // Don't override bar() here!
    }

    @SuppressWarnings("ClassTooDeepInInheritanceTree")
    static class SubSubClass extends SubClass {
        // Don't override foo() here!

        @Override
        public String bar() {
            return "SubSubClass.bar()";
        }
    }
}
