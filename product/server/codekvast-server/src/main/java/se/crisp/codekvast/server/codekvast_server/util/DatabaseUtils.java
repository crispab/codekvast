package se.crisp.codekvast.server.codekvast_server.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;

import javax.sql.DataSource;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class DatabaseUtils {

    public static boolean isMemoryDatabase(DataSource dataSource) {
        return ((PoolConfiguration) dataSource).getUrl().contains(":mem:");
    }

    public static void backupDatabase(JdbcTemplate jdbcTemplate, String backupFile) throws SQLException {
        String sql = String.format("SCRIPT NOPASSWORDS DROP TO '%s' COMPRESSION ZIP CHARSET 'UTF-8' ", backupFile);
        jdbcTemplate.execute(sql);
    }

    public static void removeOldBackups(CodekvastSettings settings, final String suffix) {
        for (File path : settings.getBackupPaths()) {
            File[] files = path.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.endsWith(suffix);
                }
            });

            if (files != null) {
                Arrays.sort(files, new FilenameComparator());

                // Now the oldest backup is first in the array, since the names start with a sortable timestamp.
                // Remove the oldest.
                for (int i = 0; i < files.length - settings.getBackupSaveGenerations(); i++) {
                    File file = files[i];
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.info("Deleted {}", file);
                    } else {
                        log.warn("Could not delete {}", file);
                    }
                }
            }
        }
    }

    public static String getBackupFile(CodekvastSettings settings, Date date, String suffix) {
        for (File path : settings.getBackupPaths()) {
            path.mkdirs();
            if (path.canWrite()) {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
                String name = String.format(Locale.ENGLISH, "%s_%s", timestamp, suffix);
                File file = new File(path, name);
                try {
                    return file.getCanonicalPath();
                } catch (IOException e) {
                    return file.getAbsolutePath();
                }
            }
        }
        throw new IllegalArgumentException(
                new IOException("Cannot create backup file in any of " + Arrays.toString(settings.getBackupPaths())));
    }

    static class FilenameComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }
}
