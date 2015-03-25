package se.crisp.codekvast.agent.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(Parameterized.class)
public class MethodVisibilityFilterTest {

    @Parameters(name = "{index}: {0}")
    public static Object[][] data() {
        //@formatter:off
        return new Object[][]{
                {null,              true, false, false, false, false, "public"},
                {"   ",             true, false, false, false, false, "public"},
                {" foobar ",        true, false, false, false, true, "public"},
                {"   ",             true, false, false, false, false, "public"},
                {"public",          true, false, false, false, false, "public"},
                {"PuBlIc",          true, false, false, false, false, "public"},
                {" public ",        true, false, false, false, false, "public"},
                {"protected",       true, true,  false, false, false, "protected"},
                {" protected ",     true, true,  false, false, false, "protected"},
                {" PROTECTED ",     true, true,  false, false, false, "protected"},
                {"package-private", true, true,  true,  false, false, "package-private"},
                {"!private",        true, true,  true,  false, false, "package-private"},
                {"private",         true, true,  true,  true, false, "private"},
                {"all",             true, true,  true,  true, false, "private"},
                {"*",               true, true,  true,  true, false, "private"},
        };
        //@formatter:on
    }

    @Parameter(0)
    public String input;

    @Parameter(1)
    public boolean selectsPublic;

    @Parameter(2)
    public boolean selectsProtected;

    @Parameter(3)
    public boolean selectsPackagePrivate;

    @Parameter(4)
    public boolean selectsPrivate;

    @Parameter(5)
    public boolean expectsSystemErr;

    @Parameter(6)
    public String expectedToString;

    private PrintStream savedSystemErr;
    private ByteArrayOutputStream capturedSystemErr = new ByteArrayOutputStream();

    @Rule
    public Verifier verifier = new Verifier() {
        @Override
        public void verify() throws IOException {
            String output = capturedSystemErr.toString();
            if (!output.isEmpty() && !expectsSystemErr) {
                fail("Unexpected output on System.err: " + output);
            }
            if (output.isEmpty() && expectsSystemErr) {
                fail("Expected output on System.err");
            }
        }
    };

    @Before
    public void beforeTest() throws Exception {
        savedSystemErr = System.err;
        System.setErr(new PrintStream(capturedSystemErr));
    }

    @After
    public void afterTest() throws Exception {
        System.setErr(savedSystemErr);
        System.err.print(capturedSystemErr.toString());
    }

    @Test
    public void shouldParseVisibility() {
        MethodVisibilityFilter mv = new MethodVisibilityFilter(input);
        assertThat("Should select public", mv.selectsPublicMethods(), is(selectsPublic));
        assertThat("Should select protected", mv.selectsProtectedMethods(), is(selectsProtected));
        assertThat("Should select package private", mv.selectsPackagePrivateMethods(), is(selectsPackagePrivate));
        assertThat("Should select private", mv.selectsPrivateMethods(), is(selectsPrivate));

        assertThat("Should include public", mv.shouldInclude(Modifier.SYNCHRONIZED | Modifier.PUBLIC), is(selectsPublic));
        assertThat("Should include protected", mv.shouldInclude(Modifier.SYNCHRONIZED | Modifier.PROTECTED), is(selectsProtected));
        assertThat("Should include package private", mv.shouldInclude(Modifier.SYNCHRONIZED | 0), is(selectsPackagePrivate));
        assertThat("Should include static package private", mv.shouldInclude(Modifier.STATIC | 0), is(selectsPackagePrivate));
        assertThat("Should include private", mv.shouldInclude(Modifier.SYNCHRONIZED | Modifier.PRIVATE), is(selectsPrivate));

        assertThat("Should normalize toString()", mv.toString(), is(expectedToString));
    }
}
