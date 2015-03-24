package se.crisp.codekvast.agent.util;

import org.junit.Test;
import se.crisp.codekvast.agent.config.MethodVisibilityFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static se.crisp.codekvast.agent.util.SignatureUtils.makeSignature;
import static se.crisp.codekvast.agent.util.SignatureUtils.signatureToString;

public class SignatureUtilsTest {

    private final MethodVisibilityFilter methodVisibilityFilter = new MethodVisibilityFilter("public");

    @Test
    public void testGetSignatureM1() throws IOException, NoSuchMethodException {
        String s = signatureToString(
                makeSignature(methodVisibilityFilter, TestClass.class, TestClass.class.getMethod("m1", String.class, Collection.class)),
                true);
        assertThat(s, is("se.crisp.codekvast.agent.util.SignatureUtilsTest.TestClass.m1(java.lang.String, java.util.Collection)"));
    }

    @Test
    public void testGetSignatureM2() throws IOException, NoSuchMethodException {
        String s = signatureToString(makeSignature(methodVisibilityFilter, TestClass.class, TestClass.class.getMethod("m2")), true);
        assertThat(s, is("se.crisp.codekvast.agent.util.SignatureUtilsTest.TestClass.m2()"));
    }

    @Test
    public void testGetSignatureM3() throws IOException, NoSuchMethodException {
        String s = signatureToString(
                makeSignature(methodVisibilityFilter, TestClass.class, TestClass.class.getMethod("m3", int.class, String[].class)), true);
        assertThat(s, is("se.crisp.codekvast.agent.util.SignatureUtilsTest.TestClass.m3(int, java.lang.String[])"));
    }

    @Test
    public void testMinimizeAlreadyMinimizedSignature() throws NoSuchMethodException {
        String s = signatureToString(
                makeSignature(methodVisibilityFilter, TestClass.class, TestClass.class.getMethod("m3", int.class, String[].class)), true);
        assertThat(s, is("se.crisp.codekvast.agent.util.SignatureUtilsTest.TestClass.m3(int, java.lang.String[])"));
        String s2 = SignatureUtils.stripModifiersAndReturnType(s);
        assertThat(s2, is(s));
    }

    @SuppressWarnings("EmptyMethod")
    public static class TestClass {
        public static Collection<List<String>> m1(String p1, Collection<Integer> p2) {
            return null;
        }

        public void m2() {

        }

        public String m3(int p1, String... args) {
            return null;
        }
    }
}
