package se.crisp.codekvast.daemon;

import se.crisp.codekvast.daemon.impl.DataExportException;

/**
 * Strategy for how to export data from the daemon.
 *
 * @author olle.hallin@crisp.se
 */
public interface DataExporter {
    /**
     * Export data from the local datastore.
     *
     * @throws DataExportException
     */
    void exportData() throws DataExportException;
}
