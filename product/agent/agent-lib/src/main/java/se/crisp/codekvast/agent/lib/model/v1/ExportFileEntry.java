package se.crisp.codekvast.agent.lib.model.v1;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.zip.ZipEntry;

/**
 * The names of the different entries in the daemon export file.
 *
 * @author olle.hallin@crisp.se
 */
@RequiredArgsConstructor
@Getter
public enum ExportFileEntry {
    DAEMON_CONFIG("codekvast-daemon.properties"),
    APPLICATIONS("applications.csv"),
    METHODS("methods.csv"),
    JVMS("jvms.csv"),
    INVOCATIONS("invocations.csv");

    private final String entryName;

    public ZipEntry toZipEntry() {
        return new ZipEntry(entryName);
    }

    public static ExportFileEntry fromString(String s) {
        for (ExportFileEntry entry : values()) {
            if (entry.entryName.equals(s)) {
                return entry;
            }
        }
        throw new IllegalArgumentException("Unrecognized export file entry: " + s);
    }
}
