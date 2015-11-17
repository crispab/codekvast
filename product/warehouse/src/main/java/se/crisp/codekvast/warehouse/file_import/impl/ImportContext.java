package se.crisp.codekvast.warehouse.file_import.impl;

import se.crisp.codekvast.warehouse.file_import.Application;

import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
public class ImportContext {
    private final Map<Long, Long> centralAppIdByLocalId = new HashMap<>();

    void putApplication(Long centralId, Application app) {
        centralAppIdByLocalId.put(app.getLocalId(), centralId);
    }

    Long getCentralAppId(Long localId) {
        return centralAppIdByLocalId.get(localId);
    }
}
