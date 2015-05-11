package se.crisp.codekvast.server.codekvast_server.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;

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

    private static final String CHARSET = "UTF-8";
    private static final String COMPRESSION = "ZIP";

    /**
     * If found in ${codekvast.backupPath}, it will be used for restoring the database.
     */
    public static final String RESTORE_ME_FILE = "restore-me." + COMPRESSION.toLowerCase();

    public static void backupDatabase(JdbcTemplate jdbcTemplate, String backupFile) throws SQLException {
        String sql = String.format("SCRIPT NOPASSWORDS DROP TO '%s' COMPRESSION %s CHARSET '%s' ", backupFile, COMPRESSION, CHARSET);
        jdbcTemplate.execute(sql);
    }

    /**
     * Looks for {@link #RESTORE_ME_FILE} in {@link CodekvastSettings#getBackupPaths()}. If found, it is used for restoring the database.
     * The file is renamed afterwards to prevent it from being restored again.
     *
     * @return true if a restore was done.
     */
    public static boolean restoreDatabaseIfRestoreMeFileWasFound(JdbcTemplate jdbcTemplate, CodekvastSettings settings) {
        boolean result = false;
        for (File path : settings.getBackupPaths()) {
            File file = new File(path, RESTORE_ME_FILE);
            if (file.isFile() && file.canRead()) {

                File renameTo;
                String timestamp = formatTimestamp(new Date());
                try {
                    log.debug(String.format("Restoring database from %s (%,d KB) ...", file, file.length() / 1024));
                    long startedAt = System.currentTimeMillis();

                    String sql = String.format("RUNSCRIPT FROM '%s' COMPRESSION %s CHARSET '%s' ", file, COMPRESSION, CHARSET);
                    jdbcTemplate.execute(sql);
                    renameTo = new File(file.getParentFile(), timestamp + "_restored-from_" + file.getName());

                    log.info("Restored database from {} in {} ms", file, System.currentTimeMillis() - startedAt);
                    result = true;
                } catch (DataAccessException e) {
                    log.error("Could not restore database from " + file, e);
                    renameTo = new File(file.getParentFile(), timestamp + "_failed-to_" + file.getName());
                }

                boolean renamed = file.renameTo(renameTo);
                if (renamed) {
                    log.debug("Renamed {} to {}", file, renameTo);
                } else {
                    log.warn("\n----------------------------------------------------------------------\n" +
                                     "  Could not rename {} to {}\n" +
                                     "  Rename or delete the file manually before restarting the server,\n" +
                                     "  or else it will be restored from once again!\n" +
                                     "----------------------------------------------------------------------",
                             file, renameTo);
                }

                return result;
            }
        }
        return false;
    }

    private static String formatTimestamp(Date date) {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
    }

    public static boolean isMemoryDatabase(JdbcTemplate jdbcTemplate) {
        return ((PoolConfiguration) jdbcTemplate.getDataSource()).getUrl().contains(":mem:");
    }

    public static void removeOldBackups(CodekvastSettings settings, String suffix) {
        final String endsWith = suffix + "." + COMPRESSION.toLowerCase();
        for (File path : settings.getBackupPaths()) {

            File[] files = path.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.endsWith(endsWith);
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

    public static String getBackupFile(CodekvastSettings settings, JdbcTemplate jdbcTemplate, Date date, String suffix) {
        return getBackupFile(settings, getSchemaVersion(jdbcTemplate), date, suffix);
    }

    public static String getBackupFile(CodekvastSettings settings, String schemaVersion, Date date, String suffix) {
        for (File path : settings.getBackupPaths()) {
            path.mkdirs();
            if (path.canWrite()) {
                String timestamp = formatTimestamp(date);
                String name = String.format(Locale.ENGLISH, "%s_%s_%s.%s", timestamp, schemaVersion, suffix, COMPRESSION.toLowerCase());
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

    public static String getSchemaVersion(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject(
                "SELECT TOP 1 \"version\" FROM \"schema_version\" ORDER BY \"version_rank\" DESC", String.class);
    }

    static class FilenameComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }
}
