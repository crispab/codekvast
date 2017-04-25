package integrationTest.warehouse.testdata;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import io.codekvast.agent.lib.model.v1.legacy.JvmData;
import io.codekvast.warehouse.file_import.ImportDAO;
import io.codekvast.warehouse.file_import.ImportDAO.Invocation;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class ImportDescriptor {
    @NonNull
    private final Long now;

    @Singular
    private final List<String> apps;

    @Singular
    private final List<MethodDescriptor> methods;

    @Singular
    private final List<JvmDataPair> jvms;

    @Singular
    private final List<Invocation> invocations;

    @Value
    public static class JvmDataPair {
        private final ImportDAO.Jvm jvm;
        private final JvmData jvmData;
    }

    @Value
    public static class MethodDescriptor {
        private final Long localId;
        private final Method method;

        public String getSignature() {
            return method.toString();
        }
    }
}
