package io.codekvast.javaagent.util;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class FileUtilsTest {

    @Test
    public void should_expand_hostname_placeholder() throws Exception {
        File file = new File("/tmp/foo-#hostname#.ser");
        File expanded = FileUtils.expandPlaceholders(file);

        assertThat(expanded.getName(), not(is(file.getName())));
    }

    @Test
    public void should_expand_timestamp_placeholder() throws Exception {
        File file = new File("/tmp/foo-#timestamp#.ser");
        File expanded = FileUtils.expandPlaceholders(file);

        assertThat(expanded.getName(), not(is(file.getName())));
    }
}
