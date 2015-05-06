package se.crisp.codekvast.server.codekvast_server.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
public class DatabaseUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TimeZone savedTimeZone;
    private CodekvastSettings settings = new CodekvastSettings();
    private Date date;

    private final String schemaVersion = "2.0";

    @Before
    public void beforeTest() throws Exception {
        savedTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));

        settings.setBackupPath(temporaryFolder.getRoot());
        date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").parse("2015-03-18 08:23:45+0000");
    }

    @After
    public void afterTest() throws Exception {
        TimeZone.setDefault(savedTimeZone);
    }

    @Test
    public void filename_should_contain_the_local_timestamp() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm"));
        File file = new File(DatabaseUtils.getBackupFile(settings, schemaVersion, date, "suffix"));
        assertThat(file.getName(), is("20150318_092345_2.0_suffix.zip"));

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        file = new File(DatabaseUtils.getBackupFile(settings, schemaVersion, date, "suffix"));
        assertThat(file.getName(), is("20150318_172345_2.0_suffix.zip"));
    }

    @Test
    public void not_writable_backup_path_should_use_fallback() throws Exception {
        settings.setBackupPath(new File("/not-existing"));

        File file = new File(DatabaseUtils.getBackupFile(settings, schemaVersion, date, "suffix"));
        assertThat(file.getParent(), is(System.getProperty("java.io.tmpdir") + "/codekvast/.backup"));
        assertThat(file.getParentFile().canWrite(), is(true));
    }

    @Test
    public void backup_path_should_be_created() throws Exception {
        settings.setBackupPath(new File(temporaryFolder.getRoot(), "not-yet-existing"));

        File file = new File(DatabaseUtils.getBackupFile(settings, schemaVersion, date, "suffix"));
        assertThat(file.getParentFile().getName(), is("not-yet-existing"));
        assertThat(file.getParentFile().canWrite(), is(true));
    }

    @Test
    public void remove_backups_should_remove_the_oldest() throws Exception {
        File[] originalFiles = createDatabaseFiles(3, "suffix");

        settings.setBackupSaveGenerations(2);
        DatabaseUtils.removeOldBackups(settings, "suffix");

        File[] remainingFiles = getRemainingFiles();
        assertThat(remainingFiles.length, is(2));

        assertThat(remainingFiles[0].getName(), is(originalFiles[1].getName()));
        assertThat(remainingFiles[1].getName(), is(originalFiles[2].getName()));
    }

    @Test
    public void remove_backups_should_not_remove_when_less_than_max() throws Exception {
        File[] originalFiles = createDatabaseFiles(3, "suffix");

        settings.setBackupSaveGenerations(4);
        DatabaseUtils.removeOldBackups(settings, "suffix");

        File[] remainingFiles = getRemainingFiles();
        assertThat(remainingFiles.length, is(3));

        assertThat(remainingFiles[0].getName(), is(originalFiles[0].getName()));
        assertThat(remainingFiles[2].getName(), is(originalFiles[2].getName()));
    }

    @Test
    public void remove_backups_should_handle_non_existing_directory() throws Exception {
        settings.setBackupPath(new File(temporaryFolder.getRoot(), "foobar"));
        DatabaseUtils.removeOldBackups(settings, "suffix");
    }

    private File[] createDatabaseFiles(int count, String suffix) throws FileNotFoundException {
        File[] result = new File[count];
        for (int i = 0; i < count; i++) {
            result[i] = createDatabaseFile(i, suffix);
        }
        return result;
    }

    private File[] getRemainingFiles() {
        File[] files = temporaryFolder.getRoot().listFiles();
        Arrays.sort(files, new DatabaseUtils.FilenameComparator());
        return files;
    }

    private File createDatabaseFile(int addHours, String suffix) throws FileNotFoundException {
        Date date = new Date(this.date.getTime() + addHours * 3600_000L);
        String file = DatabaseUtils.getBackupFile(settings, schemaVersion, date, suffix);

        PrintWriter out = new PrintWriter(new FileOutputStream(file));
        out.println(file);
        out.close();

        return new File(file);
    }

}
