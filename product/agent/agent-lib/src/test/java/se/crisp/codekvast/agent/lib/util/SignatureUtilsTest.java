package se.crisp.codekvast.agent.lib.util;

import org.junit.Test;
import se.crisp.codekvast.agent.lib.config.MethodAnalyzer;
import se.crisp.codekvast.agent.lib.model.MethodSignature;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static se.crisp.codekvast.agent.lib.util.SignatureUtils.*;

@SuppressWarnings("ALL")
public class SignatureUtilsTest {

    private final MethodAnalyzer methodAnalyzer = new MethodAnalyzer("all");

    private final Method testMethods[] = TestClass.class.getDeclaredMethods();
    private final Constructor testConstructors[] = TestClass.class.getDeclaredConstructors();

    private Method findTestMethod(String name) {
        for (Method method : testMethods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown test method: " + name);
    }

    private Constructor findTestConstructor(String name) {
        for (Constructor ctor : testConstructors) {
            if (ctor.toString().contains(name)) {
                return ctor;
            }
        }
        throw new IllegalArgumentException("Unknown test constructor: " + name);
    }

    @Test
    public void testGetSignaturePublicStaticMethod() throws IOException, NoSuchMethodException {
        String s = signatureToString(makeSignature(TestClass.class, findTestMethod("publicStaticMethod1")), true);
        assertThat(s,
                   is("public se.crisp.codekvast.agent.lib.util.SignatureUtilsTest.TestClass.publicStaticMethod1(java.lang.String, java" +
                                 ".util.Collection)"));
    }

    @Test
    public void testGetSignatureProtectedMethod2() throws IOException, NoSuchMethodException {
        String s = signatureToString(makeSignature(TestClass.class, findTestMethod("protectedMethod2")), true);
        assertThat(s, is("protected se.crisp.codekvast.agent.lib.util.SignatureUtilsTest.TestClass.protectedMethod2()"));
    }

    @Test
    public void testGetSignaturePrivateMethod3() throws IOException, NoSuchMethodException {
        String s = signatureToString(
                makeSignature(TestClass.class, findTestMethod("privateMethod3")), true);
        assertThat(s, is("private se.crisp.codekvast.agent.lib.util.SignatureUtilsTest.TestClass.privateMethod3(int, java.lang.String[])"));
    }

    @Test
    public void testGetSignaturePackagePrivateMethod4() throws IOException, NoSuchMethodException {
        String s = signatureToString(
                makeSignature(TestClass.class, findTestMethod("packagePrivateMethod4")), true);
        assertThat(s, is("package-private se.crisp.codekvast.agent.lib.util.SignatureUtilsTest.TestClass.packagePrivateMethod4(int)"));
    }

    @Test
    public void testMinimizeAlreadyMinimizedSignature() throws NoSuchMethodException {
        String s = signatureToString(
                makeSignature(TestClass.class, findTestMethod("privateMethod3")), true);
        assertThat(s, is("private se.crisp.codekvast.agent.lib.util.SignatureUtilsTest.TestClass.privateMethod3(int, java.lang.String[])"));
        String s2 = SignatureUtils.stripModifiersAndReturnType(s);
        assertThat(s2, is(s));
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void testMakeMethodSignature2() throws Exception {
        MethodSignature signature = makeMethodSignature(TestClass.class, findTestMethod("protectedMethod2"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(),
                   is("protected se.crisp.codekvast.agent.lib.util.SignatureUtilsTest.TestClass.protectedMethod2()"));
        assertThat(signature.getDeclaringType(), is("se.crisp.codekvast.agent.lib.util.SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is(""));
        assertThat(signature.getMethodName(), is("protectedMethod2"));
        assertThat(signature.getModifiers(), is("protected"));
        assertThat(signature.getPackageName(), is("se.crisp.codekvast.agent.lib.util"));
        assertThat(signature.getParameterTypes(), is(""));
        assertThat(signature.getReturnType(), is("java.lang.Integer"));
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void testMakeMethodSignature5() throws Exception {
        MethodSignature signature = makeMethodSignature(TestClass.class, findTestMethod("protectedMethod5"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(),
                   is("protected se.crisp.codekvast.agent.lib.util.SignatureUtilsTest.TestClass.protectedMethod5(java.lang.String, se" +
                              ".crisp" +
                              ".codekvast.agent.lib.util.SignatureUtilsTest.TestInterface)"));
        assertThat(signature.getDeclaringType(), is("se.crisp.codekvast.agent.lib.util.SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is("java.lang.UnsupportedOperationException"));
        assertThat(signature.getMethodName(), is("protectedMethod5"));
        assertThat(signature.getModifiers(), is("protected final strictfp"));
        assertThat(signature.getPackageName(), is("se.crisp.codekvast.agent.lib.util"));
        assertThat(signature.getParameterTypes(),
                   is("java.lang.String, se.crisp.codekvast.agent.lib.util.SignatureUtilsTest$TestInterface"));
        assertThat(signature.getReturnType(), is("int"));
    }

    @Test
    public void testMakeConstructorSignature1() throws Exception {
        MethodSignature signature = makeConstructorSignature(TestClass.class, findTestConstructor("TestClass()"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(), is("public se.crisp.codekvast.agent.lib.util.SignatureUtilsTest.TestClass()"));
        assertThat(signature.getDeclaringType(), is("se.crisp.codekvast.agent.lib.util.SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is(""));
        assertThat(signature.getMethodName(), is("<init>"));
        assertThat(signature.getModifiers(), is("public"));
        assertThat(signature.getPackageName(), is("se.crisp.codekvast.agent.lib.util"));
        assertThat(signature.getParameterTypes(), is(""));
        assertThat(signature.getReturnType(), is(""));
    }

    @Test
    public void testMakeConstructorSignature2() throws Exception {
        MethodSignature signature = makeConstructorSignature(TestClass.class, findTestConstructor("TestClass(int)"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(), is("protected se.crisp.codekvast.agent.lib.util.SignatureUtilsTest.TestClass(int)"));
        assertThat(signature.getDeclaringType(), is("se.crisp.codekvast.agent.lib.util.SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is(""));
        assertThat(signature.getMethodName(), is("<init>"));
        assertThat(signature.getModifiers(), is("protected"));
        assertThat(signature.getPackageName(), is("se.crisp.codekvast.agent.lib.util"));
        assertThat(signature.getParameterTypes(), is("int"));
        assertThat(signature.getReturnType(), is(""));
    }

    @Test
    public void testMakeConstructorSignature3() throws Exception {
        MethodSignature signature = makeConstructorSignature(TestClass.class, findTestConstructor("TestClass(int,int)"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(),
                   is("package-private se.crisp.codekvast.agent.lib.util.SignatureUtilsTest.TestClass(int, int)"));
        assertThat(signature.getDeclaringType(), is("se.crisp.codekvast.agent.lib.util.SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is("java.lang.UnsupportedOperationException"));
        assertThat(signature.getMethodName(), is("<init>"));
        assertThat(signature.getModifiers(), is(""));
        assertThat(signature.getPackageName(), is("se.crisp.codekvast.agent.lib.util"));
        assertThat(signature.getParameterTypes(), is("int, int"));
        assertThat(signature.getReturnType(), is(""));
    }

    @SuppressWarnings("unused")
    public interface TestInterface {
        void foo();
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    public static class TestClass {
        private final int i;
        private final int j;

        TestClass(int i, int j) throws UnsupportedOperationException {
            this.i = i;
            this.j = j;
        }

        public TestClass() {
            this(0, 0);
        }

        protected TestClass(int i) {
            this(i, 0);
        }

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
