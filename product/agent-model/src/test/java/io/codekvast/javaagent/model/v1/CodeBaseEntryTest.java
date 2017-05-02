package io.codekvast.javaagent.model.v1;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author olle.hallin@crisp.se
 */
public class CodeBaseEntryTest {

    @Test
    public void should_extract_public_method_signature() throws Exception {
        CodeBaseEntry entry =
            new CodeBaseEntry("public final strictfp void foo.bar.Someclass.foo()", MethodSignature.createSampleMethodSignature(), SignatureStatus.NOT_INVOKED);

        assertThat(entry.getVisibility(), is("public"));
        assertThat(entry.getSignature(), is("foo.bar.Someclass.foo()"));
    }

    @Test
    public void should_extract_public_constructor_signature() throws Exception {
        CodeBaseEntry entry =
            new CodeBaseEntry("static public foo.bar.Someclass()", MethodSignature.createSampleMethodSignature(), SignatureStatus.NOT_INVOKED);

        assertThat(entry.getVisibility(), is("public"));
        assertThat(entry.getSignature(), is("foo.bar.Someclass()"));
    }

    @Test
    public void should_extract_package_private_method_signature() throws Exception {
        CodeBaseEntry entry =
            new CodeBaseEntry("int foo.bar.Someclass.foo()", MethodSignature.createSampleMethodSignature(), SignatureStatus.NOT_INVOKED);

        assertThat(entry.getVisibility(), is("package-private"));
        assertThat(entry.getSignature(), is("foo.bar.Someclass.foo()"));
    }

    @Test
    public void should_extract_package_private_constructor_signature() throws Exception {
        CodeBaseEntry entry =
            new CodeBaseEntry("foo.bar.Someclass()", MethodSignature.createSampleMethodSignature(), SignatureStatus.NOT_INVOKED);

        assertThat(entry.getVisibility(), is("package-private"));
        assertThat(entry.getSignature(), is("foo.bar.Someclass()"));
    }
}