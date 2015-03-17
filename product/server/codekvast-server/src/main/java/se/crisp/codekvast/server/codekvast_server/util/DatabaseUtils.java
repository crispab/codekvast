package se.crisp.codekvast.server.codekvast_server.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class DatabaseUtils {

    public static boolean isMemoryDatabase(DataSource dataSource) {
        return ((PoolConfiguration) dataSource).getUrl().contains(":mem");
    }

    public static void backupDatabase(DataSource dataSource, String backupFile) throws SQLException {
        String sql = String.format("SCRIPT NOPASSWORDS DROP TO '%s' COMPRESSION ZIP CHARSET 'UTF-8' ", backupFile);
        dataSource.getConnection().createStatement().execute(sql);
    }

    public static String getBackupFile(CodekvastSettings settings, Date date, String suffix) {
        File[] backupPaths = {settings.getBackupPath(), new File("/tmp/codekvast/.backup")};

        for (File path : backupPaths) {
            path.mkdirs();
            if (path.canWrite()) {
                return new File(path, String.format(Locale.ENGLISH, "%1$tF_%1$tT_%2$s.zip", date, suffix)
                                            .replaceAll("[-:]", ""))
                        .getAbsolutePath();
            }
        }
        throw new IllegalArgumentException(new IOException("Cannot create backup file in any of " + Arrays.toString(backupPaths)));
    }
}
