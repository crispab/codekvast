package duck.spike;

import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.runtime.reflect.Factory;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Olle Hallin
 */
public class UsageRegistry {

    private static class Usage {
        private final Signature signature;
        private final long usedAtMillis;

        private Usage(Signature signature, long usedAtMillis) {
            this.signature = signature;
            this.usedAtMillis = usedAtMillis;
        }

        @Override
        public String toString() {
            return String.format("%15d %s", usedAtMillis, signature.toLongString());
        }
    }

    private UsageRegistry() {
        // Utility class with only static methods
    }

    private static ConcurrentMap<String, Usage> trackedMethods = new ConcurrentHashMap<String, Usage>();

    /**
     * This method is invoked by {@link duck.spike.AbstractDuckAspect#recordMethodCall(org.aspectj.lang.JoinPoint)}.
     * It will exclude a certain type and method from being reported as useless.
     * <p/>
     * Thread-safe.
     */
    public static void registerMethodExecution(Signature signature) {
        trackedMethods.put(makeMethodKey(signature), new Usage(signature, System.currentTimeMillis()));
    }

    private static String makeMethodKey(Signature signature) {
        return signature.toLongString();
    }

    /**
     * Dumps unused methods on System.out.
     * <p/>
     * Thread-safe.
     */
    public static void dumpUnusedCode(String packagePrefix) {
        System.out.printf("--------------------------------------------------------------%n" +
                                  "Duck result:%n");

        detectUnusedMethods(packagePrefix);

        System.out.println("Useless methods:");
        for (Usage usage : trackedMethods.values()) {
            if (usage.usedAtMillis == 0L) {
                System.out.println(usage);
            }
        }

        System.out.println("Used methods (at System.currentTimeMillis()):");
        for (Usage usage : trackedMethods.values()) {
            if (usage.usedAtMillis > 0L) {
                System.out.println(usage);
            }
        }
    }

    private static void detectUnusedMethods(String packagePrefix) {
        long startedAt = System.currentTimeMillis();

        Reflections reflections = new Reflections(ClasspathHelper.forPackage(packagePrefix), new SubTypesScanner(false));

        int count = 0;
        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isSynthetic()) {
                    // Use AspectJ for creating the same signature as AbstractDuckAspect...
                    MethodSignature signature = new Factory(null, clazz)
                            .makeMethodSig(method.getModifiers(), method.getName(), method.getDeclaringClass(), method.getParameterTypes(),
                                           null, method.getExceptionTypes(), method.getReturnType());

                    trackedMethods.putIfAbsent(makeMethodKey(signature), new Usage(signature, 0L));
                    count += 1;
                }
            }
        }

        System.out.printf("Classpath with package prefix '%s' scanned in %d ms, found %d methods.%n",
                          packagePrefix, System.currentTimeMillis() - startedAt, count);
    }

}
