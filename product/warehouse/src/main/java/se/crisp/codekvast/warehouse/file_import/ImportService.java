package se.crisp.codekvast.warehouse.file_import;

import se.crisp.codekvast.agent.lib.model.v1.ExportFileMetaInfo;

/**
 * Service for data import.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
public interface ImportService {

    boolean isFileImported(ExportFileMetaInfo metaInfo);

    void recordFileAsImported(ExportFileMetaInfo metaInfo);
}
