package se.crisp.codekvast.agent.util;

import org.junit.Test;
import se.crisp.codekvast.agent.config.MethodFilter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static se.crisp.codekvast.agent.util.SignatureUtils.makeSignature;
import static se.crisp.codekvast.agent.util.SignatureUtils.signatureToString;

public class SignatureUtilsTest {

    private final MethodFilter methodFilter = new MethodFilter("all");

    private final Method testMethods[] = TestClass.class.getDeclaredMethods();

    private Method findTestMethod(String name) {
        for (Method method : testMethods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown test method: " + name);
    }

    @Test
    public void testGetSignaturePublicStaticMethod() throws IOException, NoSuchMethodException {
        String s = signatureToString(makeSignature(methodFilter, TestClass.class, findTestMethod("publicStaticMethod1")), true);
        assertThat(s, is("public se.crisp.codekvast.agent.util.SignatureUtilsTest.TestClass.publicStaticMethod1(java.lang.String, java" +
                                 ".util.Collection)"));
    }

    @Test
    public void testGetSignatureProtectedMethod2() throws IOException, NoSuchMethodException {
        String s = signatureToString(makeSignature(methodFilter, TestClass.class, findTestMethod("protectedMethod2")), true);
        assertThat(s, is("protected se.crisp.codekvast.agent.util.SignatureUtilsTest.TestClass.protectedMethod2()"));
    }

    @Test
    public void testGetSignaturePrivateMethod3() throws IOException, NoSuchMethodException {
        String s = signatureToString(
                makeSignature(methodFilter, TestClass.class, findTestMethod("privateMethod3")), true);
        assertThat(s, is("private se.crisp.codekvast.agent.util.SignatureUtilsTest.TestClass.privateMethod3(int, java.lang.String[])"));
    }

    @Test
    public void testGetSignaturePackagePrivateMethod4() throws IOException, NoSuchMethodException {
        String s = signatureToString(
                makeSignature(methodFilter, TestClass.class, findTestMethod("packagePrivateMethod4")), true);
        assertThat(s, is("package-private se.crisp.codekvast.agent.util.SignatureUtilsTest.TestClass.packagePrivateMethod4(int)"));
    }

    @Test
    public void testMinimizeAlreadyMinimizedSignature() throws NoSuchMethodException {
        String s = signatureToString(
                makeSignature(methodFilter, TestClass.class, findTestMethod("privateMethod3")), true);
        assertThat(s, is("private se.crisp.codekvast.agent.util.SignatureUtilsTest.TestClass.privateMethod3(int, java.lang.String[])"));
        String s2 = SignatureUtils.stripModifiersAndReturnType(s);
        assertThat(s2, is(s));
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    public static class TestClass {
        public static Collection<List<String>> publicStaticMethod1(String p1, Collection<Integer> p2) {
            return null;
        }

        protected void protectedMethod2() {
        }

        private String privateMethod3(int p1, String... args) {
            return null;
        }

        int packagePrivateMethod4(int p1) {
            return 0;
        }
    }
}
