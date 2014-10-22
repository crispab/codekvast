package se.crisp.codekvast.server.codekvast_server.dao;

import se.crisp.codekvast.server.agent.model.v1.JvmRunData;
import se.crisp.codekvast.server.codekvast_server.exceptions.CodekvastException;

/**
 * @author Olle Hallin
 */
public interface StorageDAO {
    void storeJvmRunData(JvmRunData jvmRunData) throws CodekvastException;
}
