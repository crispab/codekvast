package se.crisp.codekvast.server.codekvast_server.controller;

import org.junit.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class SignatureHandlerTest {

    @Test
    public void testAddParentPackages() throws Exception {
        Set<String> packages = new TreeSet<>();
        packages.add("a.b.c.d.e1");
        packages.add("a.b.c.d.e2");
        SignatureHandler.addParentPackages(packages);
        assertThat(packages, contains("", "a", "a.b", "a.b.c", "a.b.c.d", "a.b.c.d.e1", "a.b.c.d.e2"));
    }

}
