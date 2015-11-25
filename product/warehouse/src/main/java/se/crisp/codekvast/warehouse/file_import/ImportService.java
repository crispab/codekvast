package se.crisp.codekvast.warehouse.file_import;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import se.crisp.codekvast.agent.lib.model.ExportFileMetaInfo;
import se.crisp.codekvast.agent.lib.model.v1.SignatureConfidence;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for data import.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
public interface ImportService {

    boolean isFileImported(ExportFileMetaInfo metaInfo);

    void recordFileAsImported(ExportFileMetaInfo metaInfo, ImportStatistics importStatistics);

    void saveApplication(Application application, ImportContext context);

    void saveMethod(Method method, ImportContext context);

    void saveJvm(Jvm jvm, ImportContext context);

    void saveInvocation(Invocation invocation, ImportContext context);

    void beginInsert();

    void endInsert();


    @Value
    @Builder
    class Application {
        @NonNull
        private final Long localId;
        @NonNull
        private final String name;
        @NonNull
        private final String version;
        @NonNull
        private final Long createdAtMillis;
    }

    @Value
    @Builder
    class Jvm {
        @NonNull
        private final Long localId;
        @NonNull
        private final String uuid;
        @NonNull
        private final Long startedAtMillis;
        @NonNull
        private final Long dumpedAtMillis;
        @NonNull
        private final String jvmDataJson;
    }

    @SuppressWarnings("ClassWithTooManyFields")
    @Value
    @Builder
    class Method {
        @NonNull
        private final Long localId;
        @NonNull
        private final String visibility;
        @NonNull
        private final String signature;
        @NonNull
        private final Long createdAtMillis;
        @NonNull
        private final String declaringType;
        @NonNull
        private final String exceptionTypes;
        @NonNull
        private final String methodName;
        @NonNull
        private final String modifiers;
        @NonNull
        private final String packageName;
        @NonNull
        private final String parameterTypes;
        @NonNull
        private final String returnType;
    }

    @Value
    @Builder
    class Invocation {
        @NonNull
        private final Long localApplicationId;
        @NonNull
        private final Long localMethodId;
        @NonNull
        private final Long localJvmId;
        @NonNull
        private final Long invokedAtMillis;
        @NonNull
        private final Long invocationCount;
        @NonNull
        private final SignatureConfidence confidence;
    }

    @Value
    @Builder
    class ImportStatistics {
        @NonNull
        private File importFile;
        @NonNull
        private Duration processingTime;
    }

    class ImportContext {
        private final Map<Long, Long> centralApplicationIdByLocalId = new HashMap<>();
        private final Map<Long, Long> centralMethodIdByLocalId = new HashMap<>();
        private final Map<Long, Long> centralJvmIdByLocalId = new HashMap<>();

        public void putApplication(Long centralId, Application app) {
            centralApplicationIdByLocalId.put(app.getLocalId(), centralId);
        }

        public void putMethod(Long centralId, Method method) {
            centralMethodIdByLocalId.put(method.getLocalId(), centralId);
        }

        public void putJvm(Long centralId, Jvm jvm) {
            centralJvmIdByLocalId.put(jvm.getLocalId(), centralId);
        }

        public long getApplicationId(Long localId) {
            return centralApplicationIdByLocalId.get(localId);
        }

        public long getMethodId(Long localId) {
            return centralMethodIdByLocalId.get(localId);
        }

        public long getJvmId(Long localId) {
            return centralJvmIdByLocalId.get(localId);
        }

    }
}
