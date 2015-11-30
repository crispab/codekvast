package se.crisp.codekvast.agent.daemon.worker;

import java.io.File;

/**
 * A strategy for how to upload an export file produced by {@link DataExporter} to a central warehouse.
 *
 * @author olle.hallin@crisp.se
 */
public interface FileUploader {

    void uploadFile(File file) throws FileUploadException;
}
