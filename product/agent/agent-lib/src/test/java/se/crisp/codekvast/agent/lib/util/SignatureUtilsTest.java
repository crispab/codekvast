package se.crisp.codekvast.agent.lib.util;

import org.junit.Test;
import se.crisp.codekvast.agent.lib.config.MethodFilter;
import se.crisp.codekvast.agent.lib.model.MethodSignature;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

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
        assertThat(s, is("public SignatureUtilsTest.TestClass.publicStaticMethod1(java.lang.String, java" +
                                 ".util.Collection)"));
    }

    @Test
    public void testGetSignatureProtectedMethod2() throws IOException, NoSuchMethodException {
        String s = signatureToString(makeSignature(methodFilter, TestClass.class, findTestMethod("protectedMethod2")), true);
        assertThat(s, is("protected SignatureUtilsTest.TestClass.protectedMethod2()"));
    }

    @Test
    public void testGetSignaturePrivateMethod3() throws IOException, NoSuchMethodException {
        String s = signatureToString(
                makeSignature(methodFilter, TestClass.class, findTestMethod("privateMethod3")), true);
        assertThat(s, is("private SignatureUtilsTest.TestClass.privateMethod3(int, java.lang.String[])"));
    }

    @Test
    public void testGetSignaturePackagePrivateMethod4() throws IOException, NoSuchMethodException {
        String s = signatureToString(
                makeSignature(methodFilter, TestClass.class, findTestMethod("packagePrivateMethod4")), true);
        assertThat(s, is("package-private SignatureUtilsTest.TestClass.packagePrivateMethod4(int)"));
    }

    @Test
    public void testMinimizeAlreadyMinimizedSignature() throws NoSuchMethodException {
        String s = signatureToString(
                makeSignature(methodFilter, TestClass.class, findTestMethod("privateMethod3")), true);
        assertThat(s, is("private SignatureUtilsTest.TestClass.privateMethod3(int, java.lang.String[])"));
        String s2 = SignatureUtils.stripModifiersAndReturnType(s);
        assertThat(s2, is(s));
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void testMakeMethodSignature2() throws Exception {
        MethodSignature signature = makeMethodSignature(methodFilter, TestClass.class, findTestMethod("protectedMethod2"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(),
                   is("protected SignatureUtilsTest.TestClass.protectedMethod2()"));
        assertThat(signature.getDeclaringType(), is("SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is(""));
        assertThat(signature.getMethodName(), is("protectedMethod2"));
        assertThat(signature.getModifiers(), is("protected"));
        assertThat(signature.getPackageName(), is("se.crisp.codekvast.collector_lib.util"));
        assertThat(signature.getParameterTypes(), is(""));
        assertThat(signature.getReturnType(), is("java.lang.Integer"));
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void testMakeMethodSignature5() throws Exception {
        MethodSignature signature = makeMethodSignature(methodFilter, TestClass.class, findTestMethod("protectedMethod5"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(),
                   is("protected SignatureUtilsTest.TestClass.protectedMethod5(java.lang.String, se.crisp" +
                              ".codekvast.collector_lib.util.SignatureUtilsTest.TestInterface)"));
        assertThat(signature.getDeclaringType(), is("SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is("java.lang.UnsupportedOperationException"));
        assertThat(signature.getMethodName(), is("protectedMethod5"));
        assertThat(signature.getModifiers(), is("protected final strictfp"));
        assertThat(signature.getPackageName(), is("se.crisp.codekvast.collector_lib.util"));
        assertThat(signature.getParameterTypes(), is("java.lang.String, SignatureUtilsTest$TestInterface"));
        assertThat(signature.getReturnType(), is("int"));
    }

    @SuppressWarnings("unused")
    public interface TestInterface {
        void foo();
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    public static class TestClass {
        public static Collection<List<String>> publicStaticMethod1(String p1, Collection<Integer> p2) {
            return null;
        }

        protected Integer protectedMethod2() {
            return null;
        }

        private String privateMethod3(int p1, String... args) {
            return null;
        }

        int packagePrivateMethod4(int p1) {
            return 0;
        }

        @SuppressWarnings("FinalMethod")
        final strictfp protected int protectedMethod5(String p1, TestInterface p2) throws UnsupportedOperationException {
            return 0;
        }
    }
}
