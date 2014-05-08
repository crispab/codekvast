package duck.spike;

import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.runtime.reflect.Factory;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Olle Hallin
 */
public class UsageRegistry {

    private UsageRegistry() {
        // Utility class with only static methods
    }

    private static ConcurrentMap<String, Boolean> invokedPackages = new ConcurrentHashMap<String, Boolean>();
    private static ConcurrentMap<String, Boolean> invokedTypes = new ConcurrentHashMap<String, Boolean>();
    private static ConcurrentMap<String, Boolean> invokedMethods = new ConcurrentHashMap<String, Boolean>();

    private static void scanClasspath(String packagePrefix) {
        long startedAt = System.currentTimeMillis();

        Reflections reflections = new Reflections(ClasspathHelper.forPackage(packagePrefix), new SubTypesScanner(false));

        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            invokedPackages.putIfAbsent(makePackageKey(clazz), Boolean.FALSE);
            invokedTypes.putIfAbsent(makeTypeKey(clazz), Boolean.FALSE);

            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isSynthetic()) {
                    // Use AspectJ for creating the same signature as AbstractDuckAspect...
                    MethodSignature signature = new Factory(null, clazz)
                            .makeMethodSig(method.getModifiers(), method.getName(), method.getDeclaringClass(), method.getParameterTypes(),
                                           null, method.getExceptionTypes(), method.getReturnType());

                    invokedMethods.putIfAbsent(makeMethodKey(signature), Boolean.FALSE);
                }
            }
        }

        System.out.printf("Classpath with package prefix '%s' scanned in %d ms, found %d packages, %d types and %d methods.%n",
                          packagePrefix, System.currentTimeMillis() - startedAt, invokedTypes.size(), invokedTypes.size(),
                          invokedMethods.size());
    }

    private static String makeTypeKey(Class<?> clazz) {
        return clazz.getName();
    }

    /**
     * This method is invoked by {@link duck.spike.AbstractDuckAspect#recordMethodCall(org.aspectj.lang.JoinPoint)}.
     * It will exclude a certain type and method from being reported as useless.
     * <p/>
     * Thread-safe.
     */
    public static void registerMethodExecution(Signature signature) {
        Class<?> declaringType = signature.getDeclaringType();

        invokedPackages.put(makePackageKey(declaringType), Boolean.TRUE);
        invokedTypes.put(makeTypeKey(declaringType), Boolean.TRUE);
        invokedMethods.put(makeMethodKey(signature), Boolean.TRUE);
    }

    private static String makeMethodKey(Signature signature) {
        return signature.toLongString();
    }

    private static String makePackageKey(Class<?> type) {
        return type.getPackage().getName();
    }

    /**
     * Dumps unused packages, types and methods on System.out.
     * <p/>
     * Thread-safe.
     */
    public static synchronized void dumpUnusedCode(String packagePrefix) {
        System.out.printf("%nDuck result:%n");

        scanClasspath(packagePrefix);

        dumpUnused("packages", invokedPackages);
        dumpUnused("types", invokedTypes);
        dumpUnused("methods", invokedMethods);
    }

    private static void dumpUnused(String what, Map<String, Boolean> invoked) {
        Collection<String> unused = new TreeSet<String>();
        for (Map.Entry<String, Boolean> entry : invoked.entrySet()) {
            if (!entry.getValue()) {
                unused.add(entry.getKey());
            }
        }

        System.out.printf("Useless %s:%n", what);
        for (String name : unused) {
            System.out.printf("  %s%n", name);
        }
    }

}
