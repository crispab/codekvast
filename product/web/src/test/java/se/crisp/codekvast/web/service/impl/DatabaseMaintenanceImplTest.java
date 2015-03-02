package se.crisp.codekvast.web.service.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class DatabaseMaintenanceImplTest {

    private static final String SUFFIX = ".h2.zip";
    private final Date T1 = new Date(1416570848627L);
    private TimeZone savedTimeZone;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void beforeTest() throws Exception {
        savedTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm"));
    }

    @After
    public void afterTest() throws Exception {
        TimeZone.setDefault(savedTimeZone);
    }

    @Test
    public void testGetDumpFile() throws Exception {
        assertThat(DatabaseMaintenanceImpl.getDumpFile(new File("foo/bar"), T1, ".h2.zip"),
                   hasToString("foo" + File.separator + "bar" + File.separator + "20141121_125408" + SUFFIX));
    }

    @Test
    public void testRemoveOldBackupsWhenEmptyFolder() throws Exception {
        DatabaseMaintenanceImpl.removeOldBackups(folder.getRoot(), 10, SUFFIX);
    }


    @Test
    public void testRemoveOldBackupsWhenLessThanMax() throws Exception {
        simulateDumpFiles(3, SUFFIX);
        simulateDumpFiles(10, SUFFIX + "X");

        DatabaseMaintenanceImpl.removeOldBackups(folder.getRoot(), 5, SUFFIX);

        File[] files = folder.getRoot().listFiles();
        Arrays.sort(files);
        assertThat(files.length, is(13));
    }

    @Test
    public void testRemoveOldBackupsWhenMoreThanMax() throws Exception {
        simulateDumpFiles(10, SUFFIX);
        simulateDumpFiles(10, SUFFIX + "X");

        DatabaseMaintenanceImpl.removeOldBackups(folder.getRoot(), 5, SUFFIX);

        File[] files = folder.getRoot().listFiles();

        assertThat(files.length, is(15));

        Arrays.sort(files);
        assertThat(files[0].getName(), endsWith("000" + SUFFIX + "X"));
        assertThat(files[4].getName(), endsWith("004" + SUFFIX + "X"));
        assertThat(files[5].getName(), endsWith("005" + SUFFIX));
        assertThat(files[6].getName(), endsWith("005" + SUFFIX + "X"));
    }

    private void simulateDumpFiles(int numDumps, String suffix) throws IOException {
        for (int i = 0; i < numDumps; i++) {
            folder.newFile(String.format("yyyymmdd_%03d%s", i, suffix));
        }
    }
}