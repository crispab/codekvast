package io.codekvast.javaagent.util;

import com.google.common.io.Files;
import io.codekvast.javaagent.config.MethodAnalyzer;
import io.codekvast.javaagent.model.v1.MethodSignature;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import static io.codekvast.javaagent.util.SignatureUtils.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ALL")
public class SignatureUtilsTest {

    private final MethodAnalyzer methodAnalyzer = new MethodAnalyzer("all");

    private final Method testMethods[] = TestClass.class.getDeclaredMethods();
    private final Constructor testConstructors[] = TestClass.class.getDeclaredConstructors();

    private final String[] byteCodeAddedMethods = {
        "public int se.customer.module.l2mgr.impl.persistence.FlowDomainFragmentLongTransactionEAO..EnhancerByGuice..969b9638." +
            ".FastClassByGuice..96f9109e.getIndex(com.google.inject.internal.cglib.core..Signature)",
        "public int se.customer.module.l1mgr.connectivity.persistence.TrailEAO..EnhancerByGuice..a219ec4a..FastClassByGuice." +
            ".2d349e96.getIndex(java.lang.Class[])",
        "public void io.codekvast.sample.codekvastspringheroku.CodekvastSampleApplication..EnhancerBySpringCGLIB..a405a15d()",
        };

    @Test
    public void should_ignore_byte_code_added_signatures() throws URISyntaxException {
        for (String s : byteCodeAddedMethods) {
            String sig = SignatureUtils.normalizeSignature(s);
            assertThat("Guice-generated method should be ignored", sig, nullValue());
        }
    }

    @Test
    public void should_normalize_strange_signatures() throws URISyntaxException, IOException {
        List<String> signatures =
            Files.readLines(new File(getClass().getResource("/customer1/signatures1.dat").toURI()), Charset.forName("UTF-8"));

        boolean inStrangeSignaturesSection = false;
        for (String signature : signatures) {
            if (signature.equals("# Raw strange signatures:")) {
                inStrangeSignaturesSection = true;
            } else if (inStrangeSignaturesSection) {
                String normalized = SignatureUtils.normalizeSignature(signature);
                if (normalized != null) {
                    assertThat(String.format("Could not normalize%n%n   %s%n%n result is%n%n   %s%n", signature, normalized),
                               SignatureUtils.isStrangeSignature(normalized), is(false));
                }
            }
        }
    }

    @Test
    public void should_strip_modifiers_publicStaticMethod1() throws IOException, NoSuchMethodException {
        String s = stripModifiersAndReturnType(signatureToString(makeSignature(TestClass.class, findTestMethod("publicStaticMethod1"))));
        assertThat(s,
                   is("public io.codekvast.javaagent.util.SignatureUtilsTest.TestClass.publicStaticMethod1(java.lang.String, java.util" +
                          ".Collection)"));
    }

    private Method findTestMethod(String name) {
        for (Method method : testMethods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown test method: " + name);
    }

    @Test
    public void should_strip_modifiers_protectedMethod2() throws IOException, NoSuchMethodException {
        String s =
            stripModifiersAndReturnType(signatureToString(makeSignature(TestClass.class, findTestMethod("protectedMethod2"))));
        assertThat(s, is("protected io.codekvast.javaagent.util.SignatureUtilsTest.TestClass.protectedMethod2()"));
    }

    @Test
    public void should_strip_modifiers_privateMethod3() throws IOException, NoSuchMethodException {
        String s = stripModifiersAndReturnType(signatureToString(makeSignature(TestClass.class, findTestMethod("privateMethod3"))));
        assertThat(s, is("private io.codekvast.javaagent.util.SignatureUtilsTest.TestClass.privateMethod3(int, java.lang.String[])"));
    }

    @Test
    public void should_strip_modifiers_privateMethod4() throws IOException, NoSuchMethodException {
        String s = stripModifiersAndReturnType(signatureToString(makeSignature(TestClass.class, findTestMethod("packagePrivateMethod4"))));
        assertThat(s, is("package-private io.codekvast.javaagent.util.SignatureUtilsTest.TestClass.packagePrivateMethod4(int)"));
    }

    @Test
    public void should_strip_modifiers_twice() throws NoSuchMethodException {
        String s = stripModifiersAndReturnType(signatureToString(makeSignature(TestClass.class, findTestMethod("privateMethod3"))));
        assertThat(s, is("private io.codekvast.javaagent.util.SignatureUtilsTest.TestClass.privateMethod3(int, java.lang.String[])"));
        String s2 = stripModifiersAndReturnType(s);
        assertThat(s2, is(s));
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void should_make_signature_for_protectedMethod2() throws Exception {
        MethodSignature signature = makeMethodSignature(TestClass.class, findTestMethod("protectedMethod2"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(),
                   is("protected io.codekvast.javaagent.util.SignatureUtilsTest.TestClass.protectedMethod2()"));
        assertThat(signature.getDeclaringType(), is("io.codekvast.javaagent.util.SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is(""));
        assertThat(signature.getMethodName(), is("protectedMethod2"));
        assertThat(signature.getModifiers(), is("protected"));
        assertThat(signature.getPackageName(), is("io.codekvast.javaagent.util"));
        assertThat(signature.getParameterTypes(), is(""));
        assertThat(signature.getReturnType(), is("java.lang.Integer"));
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void should_make_signature_for_protectedMethod5() throws Exception {
        MethodSignature signature = makeMethodSignature(TestClass.class, findTestMethod("protectedMethod5"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(),
                   is("protected io.codekvast.javaagent.util.SignatureUtilsTest.TestClass.protectedMethod5(java.lang.String, io.codekvast" +
                          ".javaagent.util.SignatureUtilsTest.TestInterface)"));
        assertThat(signature.getDeclaringType(), is("io.codekvast.javaagent.util.SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is("java.lang.UnsupportedOperationException"));
        assertThat(signature.getMethodName(), is("protectedMethod5"));
        assertThat(signature.getModifiers(), is("protected final strictfp"));
        assertThat(signature.getPackageName(), is("io.codekvast.javaagent.util"));
        assertThat(signature.getParameterTypes(),
                   is("java.lang.String, io.codekvast.javaagent.util.SignatureUtilsTest$TestInterface"));
        assertThat(signature.getReturnType(), is("int"));
    }

    @Test
    public void should_make_signature_for_TestClass_constructor() throws Exception {
        MethodSignature signature = makeConstructorSignature(TestClass.class, findTestConstructor("TestClass()"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(), is("public io.codekvast.javaagent.util.SignatureUtilsTest.TestClass()"));
        assertThat(signature.getDeclaringType(), is("io.codekvast.javaagent.util.SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is(""));
        assertThat(signature.getMethodName(), is("<init>"));
        assertThat(signature.getModifiers(), is("public"));
        assertThat(signature.getPackageName(), is("io.codekvast.javaagent.util"));
        assertThat(signature.getParameterTypes(), is(""));
        assertThat(signature.getReturnType(), is(""));
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
    public void should_make_signature_for_TestClass_constructor_int() throws Exception {
        MethodSignature signature = makeConstructorSignature(TestClass.class, findTestConstructor("TestClass(int)"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(), is("protected io.codekvast.javaagent.util.SignatureUtilsTest.TestClass(int)"));
        assertThat(signature.getDeclaringType(), is("io.codekvast.javaagent.util.SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is(""));
        assertThat(signature.getMethodName(), is("<init>"));
        assertThat(signature.getModifiers(), is("protected"));
        assertThat(signature.getPackageName(), is("io.codekvast.javaagent.util"));
        assertThat(signature.getParameterTypes(), is("int"));
        assertThat(signature.getReturnType(), is(""));
    }

    @Test
    public void should_make_signature_for_TestClass_constructor_int_int() throws Exception {
        MethodSignature signature = makeConstructorSignature(TestClass.class, findTestConstructor("TestClass(int,int)"));
        assertThat(signature, notNullValue());
        assertThat(signature.getAspectjString(),
                   is("package-private io.codekvast.javaagent.util.SignatureUtilsTest.TestClass(int, int)"));
        assertThat(signature.getDeclaringType(), is("io.codekvast.javaagent.util.SignatureUtilsTest$TestClass"));
        assertThat(signature.getExceptionTypes(), is("java.lang.UnsupportedOperationException"));
        assertThat(signature.getMethodName(), is("<init>"));
        assertThat(signature.getModifiers(), is(""));
        assertThat(signature.getPackageName(), is("io.codekvast.javaagent.util"));
        assertThat(signature.getParameterTypes(), is("int, int"));
        assertThat(signature.getReturnType(), is(""));
    }

    @Test
    public void should_strip_modifiers_when_no_lparen() throws Exception {
        String signature = "public synchronized java.lang.String com.acme.Foo.bar";
        assertThat(stripModifiers(signature), is("com.acme.Foo.bar"));
    }

    @Test
    public void should_strip_modifiers_when_lparen() throws Exception {
        String signature = "public synchronized java.lang.String com.acme.Foo.bar()";
        assertThat(stripModifiers(signature), is("com.acme.Foo.bar()"));
    }

    @Test
    public void should_strip_modifiers_and_return_type_when_no_lparen() throws Exception {
        String signature = "public synchronized java.lang.String foobar";
        assertThat(stripModifiersAndReturnType(signature), is("public foobar"));
    }

    @Test
    public void should_detect_visibilities() {
        assertThat(SignatureUtils.getVisibility("foo public bar"), is("public"));
        assertThat(SignatureUtils.getVisibility("foo protected bar"), is("protected"));
        assertThat(SignatureUtils.getVisibility("foo package-private bar"), is("package-private"));
        assertThat(SignatureUtils.getVisibility("foo private bar"), is("private"));
        assertThat(SignatureUtils.getVisibility("foo bar"), is("package-private"));
    }

    @SuppressWarnings("unused")
    public interface TestInterface {
        void foo();
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    public static class TestClass {
        private final int i;
        private final int j;

        public TestClass() {
            this(0, 0);
        }

        TestClass(int i, int j) throws UnsupportedOperationException {
            this.i = i;
            this.j = j;
        }

        protected TestClass(int i) {
            this(i, 0);
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

        public static Collection<List<String>> publicStaticMethod1(String p1, Collection<Integer> p2) {
            return null;
        }
    }
}
