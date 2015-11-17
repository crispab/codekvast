package se.crisp.codekvast.warehouse.file_import.impl;

import se.crisp.codekvast.warehouse.file_import.Application;
import se.crisp.codekvast.warehouse.file_import.Jvm;
import se.crisp.codekvast.warehouse.file_import.Method;

import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
public class ImportContext {
    private final Map<Long, Long> centralAppIdByLocalId = new HashMap<>();
    private final Map<Long, Long> centralMethodIdByLocalId = new HashMap<>();
    private final Map<Long, Long> centralJvmIdByLocalId = new HashMap<>();

    void putApplication(Long centralId, Application app) {
        centralAppIdByLocalId.put(app.getLocalId(), centralId);
    }

    void putMethod(Long centralId, Method method) {
        centralMethodIdByLocalId.put(method.getLocalId(), centralId);
    }

    void putJvm(Long centralId, Jvm jvm) {
        centralJvmIdByLocalId.put(jvm.getLocalId(), centralId);
    }
}
