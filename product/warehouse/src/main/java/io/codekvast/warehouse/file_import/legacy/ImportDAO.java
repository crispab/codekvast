/*
 * Copyright (c) 2015-2017 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.warehouse.file_import.legacy;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import io.codekvast.agent.lib.model.v1.legacy.ExportFileMetaInfo;
import io.codekvast.agent.lib.model.v1.legacy.JvmData;
import io.codekvast.agent.lib.model.v1.SignatureStatus;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for data import.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
public interface ImportDAO {

    /**
     *
     * @param metaInfo Meta info about one export file received from an agent.
     * @return true iff the file already has been imported.
     */
    boolean isFileImported(ExportFileMetaInfo metaInfo);

    /**
     * Marks an import file as processed.
     * @param metaInfo The meta data about the import file.
     * @param importStatistics Statistics collected during the import.
     */
    void recordFileAsImported(ExportFileMetaInfo metaInfo, ImportStatistics importStatistics);

    /**
     * An idempotent method for adding an application to the database.
     *
     * @param application The application being added.
     * @param context The import context.
     * @return true iff the application was actually inserted in the database.
     */
    boolean saveApplication(Application application, ImportContext context);

    /**
     * An idempotent method for adding a method to the database.
     *
     * @param method The method being saved.
     * @param context The import context.
     * @return true iff the method was actually inserted in the database.
     */
    boolean saveMethod(Method method, ImportContext context);

    /**
     * An idempotent method for adding a jvm to the database.
     *
     * @param jvm The JVM being saved.
     * @param jvmData Data about the JVM being saved.
     * @param context The import context.
     * @return true iff the jvm was actually inserted in the database.
     */
    boolean saveJvm(Jvm jvm, JvmData jvmData, ImportContext context);

    /**
     * An idempotent method for adding an invocation to the database.
     *
     * @param invocation The invocation being saved.
     * @param context The import context.
     * @return true iff the invocation was actually inserted or updated in the database.
     */
    boolean saveInvocation(Invocation invocation, ImportContext context);

    @Value
    @Builder(toBuilder = true)
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
    @Builder(toBuilder = true)
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
    @Builder(toBuilder = true)
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
    @Builder(toBuilder = true)
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
        private final SignatureStatus status;
    }

    @Value
    @Builder
    class ImportStatistics {
        @NonNull
        private File importFile;
        @NonNull
        private String fileSize;
        @NonNull
        private Duration processingTime;
    }

    class ImportContext {
        private final Map<Long, Long> centralApplicationIdByLocalId = new HashMap<>();
        private final Map<Long, Long> centralMethodIdByLocalId = new HashMap<>();
        private final Map<Long, Long> centralJvmIdByLocalId = new HashMap<>();

        void putApplication(Long centralId, Application app) {
            centralApplicationIdByLocalId.put(app.getLocalId(), centralId);
        }

        void putMethod(Long centralId, Method method) {
            centralMethodIdByLocalId.put(method.getLocalId(), centralId);
        }

        void putJvm(Long centralId, Jvm jvm) {
            centralJvmIdByLocalId.put(jvm.getLocalId(), centralId);
        }

        public long getApplicationId(Long localId) {
            return centralApplicationIdByLocalId.get(localId);
        }

        public long getMethodId(Long localId) {
            return centralMethodIdByLocalId.get(localId);
        }

        long getJvmId(Long localId) {
            return centralJvmIdByLocalId.get(localId);
        }

    }
}
