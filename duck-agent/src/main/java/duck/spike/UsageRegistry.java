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

/**
 * @author Olle Hallin
 */
public class UsageRegistry {

    private UsageRegistry() {
        // Utility class with only static methods
    }

    private static Map<String, Boolean> invokedPackages = new ConcurrentHashMap<String, Boolean>();
    private static Map<String, Boolean> invokedTypes = new ConcurrentHashMap<String, Boolean>();
    private static Map<String, Boolean> invokedMethods = new ConcurrentHashMap<String, Boolean>();

    /**
     * Scans the classpath for types in packages starting with packagePrefix, and initializes internal usage data for fast,
     * thread-safe usage data recording.
     * All detected types and methods are initially in unused (useless) state.
     * <p/>
     * NOTE: This method must <em>not</em> be invoked before loading the aspectjweaver, or else Duck will not work!
     * <p/>
     * Thread-safe.
     */
    public synchronized static void scanClasspath(String packagePrefix) {
        long startedAt = System.currentTimeMillis();

        invokedPackages.clear();
        invokedTypes.clear();
        invokedMethods.clear();

        Reflections reflections = new Reflections(ClasspathHelper.forPackage(packagePrefix), new SubTypesScanner(false));

        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            invokedPackages.put(clazz.getPackage().getName(), Boolean.FALSE);
            invokedTypes.put(clazz.getName(), Boolean.FALSE);

            for (Method method : clazz.getDeclaredMethods()) {
                if (isNotAspectJInjectedMethod(method)) {
                    // Use AspectJ for creating the same signature as AbstractDuckAspect...
                    MethodSignature signature = new Factory(null, clazz)
                            .makeMethodSig(method.getModifiers(), method.getName(), method.getDeclaringClass(), method.getParameterTypes(),
                                           null, method.getExceptionTypes(), method.getReturnType());

                    invokedMethods.put(signature.toLongString(), Boolean.FALSE);
                }
            }
        }

        System.out.printf("Classpath with package prefix '%s' scanned in %d ms, found %d packages, %d types and %d methods.%n",
                          packagePrefix, System.currentTimeMillis() - startedAt, invokedTypes.size(), invokedTypes.size(),
                          invokedMethods.size());
    }

    private static boolean isNotAspectJInjectedMethod(Method method) {
        return !method.isSynthetic();
    }

    /**
     * This method is invoked by {@link duck.spike.AbstractDuckAspect#recordMethodCall(org.aspectj.lang.JoinPoint)}.
     * It will exclude a certain type and method from being reported as useless.
     * <p/>
     * Thread-safe.
     */
    public static void registerMethodExecution(Signature signature) {
        assert !invokedMethods.isEmpty() : "Must invoke scanClasspath(String packagePrefix) first!";

        Class<?> declaringType = signature.getDeclaringType();

        invokedPackages.put(declaringType.getPackage().getName(), Boolean.TRUE);
        invokedTypes.put(declaringType.getName(), Boolean.TRUE);
        invokedMethods.put(signature.toLongString(), Boolean.TRUE);
    }

    /**
     * Dumps unused packages, types and methods on System.out.
     * <p/>
     * Thread-safe.
     */
    public static synchronized void dumpUnusedCode() {
        System.out.printf("%nDuck result:%n");
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
