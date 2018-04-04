package io.codekvast.javaagent.appversion;

import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PropertiesAppVersionStrategyTest {

    private final Collection<File> VALID_URIS = Collections.emptyList();
    private final String VALID_FILE;
    private final String INVALID_FILE;

    private final AppVersionStrategy strategy = new PropertiesAppVersionStrategy();

    public PropertiesAppVersionStrategyTest() {
        VALID_FILE = getClass().getResource("/PropertiesAppVersionStrategyTest.conf").getPath();
        INVALID_FILE = "NOT_FOUND";
    }

    @Test
    public void testResolveWhen_validSingleProperty() {
        String args[] = {"properties", VALID_FILE, "version"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is("1.2.3"));
    }

    @Test
    public void testResolveWhen_validDualProperties() {
        String args[] = {"properties", VALID_FILE, "version", "build"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is("1.2.3-4711"));
    }

    @Test
    public void testResolveWhen_invalidFileName() {
        String args[] = {"properties", INVALID_FILE, "version", "build"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is("<unknown>"));
    }

    @Test
    public void testResolveWhen_validFirstProperty_and_invalidSecondProperty() {
        String args[] = {"properties", VALID_FILE, "version", "foobar"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is("1.2.3"));
    }

    @Test
    public void testResolveWhen_invalidFirstProperty_and_validSecondProperty() {
        String args[] = {"properties", VALID_FILE, "foobar", "version"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is("1.2.3"));
    }

}
