package duck.spike;

import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.runtime.reflect.Factory;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Olle Hallin
 */
public class UsageRegistry {

    private UsageRegistry() {
        // Utility class with only static methods
    }

    private static String packagePrefix;
    private static Map<String, Boolean> invokedTypes;
    private static Map<String, Boolean> invokedMethods;

    public static void registerMethodExecution(String declaringType, String methodSignature) {
        if (invokedTypes == null) {
            scanClassPathForPublicMethods();
        }
        invokedTypes.put(declaringType, true);
        invokedMethods.put(methodSignature, true);
    }

    private static synchronized void scanClassPathForPublicMethods() {
        invokedTypes = new ConcurrentHashMap<String, Boolean>();
        invokedMethods = new ConcurrentHashMap<String, Boolean>();

        Reflections reflections = new Reflections(ClasspathHelper.forPackage(packagePrefix), new SubTypesScanner(false));

        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            invokedTypes.put(clazz.getName(), false);

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.toGenericString().contains("public")) {
                    MethodSignature signature = new Factory(null, clazz).makeMethodSig(
                            method.getModifiers(),
                            method.getName(),
                            method.getDeclaringClass(),
                            method.getParameterTypes(),
                            null,
                            method.getExceptionTypes(),
                            method.getReturnType());
                    invokedMethods.put(signature.toLongString(), false);
                }
            }
        }
    }

    public static void setPackagePrefix(String packagePrefix) {
        UsageRegistry.packagePrefix = packagePrefix;
    }

    public static void dumpUnusedCode() {
        if (invokedTypes != null) {
            System.out.printf("%nDumping code usage:%n");
            detectUnused("types", invokedTypes);
            detectUnused("methods", invokedMethods);
        }
    }

    private static void detectUnused(String what, Map<String, Boolean> invoked) {
        Set<String> unused = new TreeSet<String>();
        for (Map.Entry<String, Boolean> entry : invoked.entrySet()) {
            if (!entry.getValue()) {
                unused.add(entry.getKey());
            }
        }

        if (!unused.isEmpty()) {
            System.out.printf("  Unused %s:%n", what);
            for (String name : unused) {
                System.out.printf("    %s%n", name);
            }
        }
    }

}
