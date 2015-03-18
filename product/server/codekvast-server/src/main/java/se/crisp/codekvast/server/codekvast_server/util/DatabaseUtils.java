package se.crisp.codekvast.server.codekvast_server.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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

    public static void backupDatabase(DataSource dataSource, String backupFile) throws SQLException {
        String sql = String.format("SCRIPT NOPASSWORDS DROP TO '%s' COMPRESSION ZIP CHARSET 'UTF-8' ", backupFile);
        dataSource.getConnection().createStatement().execute(sql);
    }

    public static String getBackupFile(CodekvastSettings settings, Date date, String suffix) {
        for (File path : settings.getBackupPaths()) {
            path.mkdirs();
            if (path.canWrite()) {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
                String name = String.format(Locale.ENGLISH, "%s_%s.zip", timestamp, suffix);
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
}
