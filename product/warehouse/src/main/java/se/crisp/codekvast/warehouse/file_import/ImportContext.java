package se.crisp.codekvast.warehouse.file_import;

import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
public class ImportContext {
    private final Map<Long, Long> applicationIdByLocalId = new HashMap<>();

    void putApplication(Long centralId, Application app) {
        applicationIdByLocalId.put(app.getId(), centralId);
    }

    Long getAppId(Long localId) {
        return applicationIdByLocalId.get(localId);
    }
}
