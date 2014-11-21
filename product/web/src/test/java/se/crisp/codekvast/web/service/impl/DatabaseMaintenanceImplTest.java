package se.crisp.codekvast.web.service.impl;

import org.junit.Test;

import java.io.File;
import java.util.Date;

import static org.hamcrest.Matchers.hasToString;
import static org.junit.Assert.assertThat;

public class DatabaseMaintenanceImplTest {

    private Date t1 = new Date(100000000000L);

    @Test
    public void testGetDumpFile() throws Exception {
        assertThat(DatabaseMaintenanceImpl.getDumpFile(new File("foo/bar"), t1), hasToString("foo/bar/19730303_104640.h2.dmp"));
    }
}