package se.crisp.codekvast.server.codekvast_server.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;

import java.io.File;
import java.text.SimpleDateFormat;
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

    @Before
    public void beforeTest() throws Exception {
        savedTimeZone = TimeZone.getDefault();
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
        File file = new File(DatabaseUtils.getBackupFile(settings, date, "suffix"));
        assertThat(file.getName(), is("20150318_092345_suffix.zip"));

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        file = new File(DatabaseUtils.getBackupFile(settings, date, "suffix"));
        assertThat(file.getName(), is("20150318_172345_suffix.zip"));
    }

    @Test
    public void not_writable_backup_path_should_use_fallback() throws Exception {
        settings.setBackupPath(new File("/not-existing"));

        File file = new File(DatabaseUtils.getBackupFile(settings, date, "suffix"));
        assertThat(file.getParent(), is(System.getProperty("java.io.tmpdir") + "/codekvast/.backup"));
        assertThat(file.getParentFile().canWrite(), is(true));
    }

    @Test
    public void backup_path_should_be_created() throws Exception {
        settings.setBackupPath(new File(temporaryFolder.getRoot(), "not-yet-existing"));

        File file = new File(DatabaseUtils.getBackupFile(settings, date, "suffix"));
        assertThat(file.getParentFile().getName(), is("not-yet-existing"));
        assertThat(file.getParentFile().canWrite(), is(true));
    }
}