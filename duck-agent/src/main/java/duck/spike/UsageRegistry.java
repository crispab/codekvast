package duck.spike;

import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.runtime.reflect.Factory;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Olle Hallin
 */
public class UsageRegistry {

    private static class Usage {
        private final Class<?> declaringClass;
        private final boolean used;

        private Usage(Class<?> declaringClass, boolean used) {
            this.declaringClass = declaringClass;
            this.used = used;
        }
    }

    private UsageRegistry() {
        // Utility class with only static methods
    }

    private static ConcurrentMap<String, Usage> invokedPackages = new ConcurrentHashMap<String, Usage>();
    private static ConcurrentMap<String, Usage> invokedTypes = new ConcurrentHashMap<String, Usage>();
    private static ConcurrentMap<String, Usage> invokedMethods = new ConcurrentHashMap<String, Usage>();

    private static void scanClasspath(String packagePrefix) {
        long startedAt = System.currentTimeMillis();

        Reflections reflections = new Reflections(ClasspathHelper.forPackage(packagePrefix), new SubTypesScanner(false));

        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            Usage notUsed = new Usage(clazz, false);
            invokedPackages.putIfAbsent(makePackageKey(clazz), notUsed);
            invokedTypes.putIfAbsent(makeTypeKey(clazz), notUsed);

            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isSynthetic()) {
                    // Use AspectJ for creating the same signature as AbstractDuckAspect...
                    MethodSignature signature = new Factory(null, clazz)
                            .makeMethodSig(method.getModifiers(), method.getName(), method.getDeclaringClass(), method.getParameterTypes(),
                                           null, method.getExceptionTypes(), method.getReturnType());

                    invokedMethods.putIfAbsent(makeMethodKey(signature), notUsed);
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

        Usage used = new Usage(declaringType, true);
        invokedPackages.put(makePackageKey(declaringType), used);
        invokedTypes.put(makeTypeKey(declaringType), used);
        invokedMethods.put(makeMethodKey(signature), used);
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
        System.out.printf("--------------------------------------------------------------%n" +
                                  "Duck result:%n");

        scanClasspath(packagePrefix);

        System.out.println("Useless packages (and all types they contain):");
        Set<Package> unusedPackages = new HashSet<Package>();
        for (Map.Entry<String, Usage> entry : invokedPackages.entrySet()) {
            if (!entry.getValue().used) {
                Package p = entry.getValue().declaringClass.getPackage();
                unusedPackages.add(p);
                System.out.printf("  %s%n", p);
            }
        }

        System.out.println("Useless types (and all methods they contain):");
        Set<Class> unusedTypes = new HashSet<Class>();
        for (Map.Entry<String, Usage> entry : invokedTypes.entrySet()) {
            Usage usage = entry.getValue();
            if (!usage.used) {
                unusedTypes.add(usage.declaringClass);
                if (!unusedPackages.contains(usage.declaringClass.getPackage())) {
                    System.out.printf("  %s%n", usage.declaringClass);
                }
            }
        }

        System.out.println("Useless methods:");
        for (Map.Entry<String, Usage> entry : invokedMethods.entrySet()) {
            Usage usage = entry.getValue();
            if (!usage.used && !unusedTypes.contains(usage.declaringClass)) {
                System.out.printf("  %s%n", entry.getKey());
            }
        }

        System.out.println("Used methods:");
        for (Map.Entry<String, Usage> entry : invokedMethods.entrySet()) {
            Usage usage = entry.getValue();
            if (usage.used) {
                System.out.printf("  %s%n", entry.getKey());
            }
        }
    }

}
