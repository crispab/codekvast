package se.crisp.codekvast.warehouse.file_import;

/**
 * Service for data import.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
public interface ImportService {

    boolean isFileImported(String uuid);

    void recordFileAsImported(String uuid, long lengthBytes, String importedFromHostname);
}
