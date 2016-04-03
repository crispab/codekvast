package se.crisp.codekvast.warehouse.file_import;

import java.io.File;

/**
 * @author olle.hallin@crisp.se
 */
public interface ZipFileImporter {
    void importZipFile(File file);
}
