package duck.spike.util;

import org.aspectj.lang.Signature;
import org.aspectj.runtime.reflect.Factory;

import java.lang.reflect.Method;

/**
 * @author Olle Hallin
 */
public class AspectjUtils {

    private AspectjUtils() {
        // utility class
    }

    public static String makeMethodKey(Signature signature) {
        return signature.toLongString();
    }

    /**
     * Use AspectJ for creating the same signature as AbstractDuckAspect.
     *
     * @return The same signature object as an AspectJ execution pointcut will provide in JoinPoint.getSignature()
     */
    public static Signature makeMethodSignature(Method method) {
        return new Factory(null, method.getDeclaringClass())
                .makeMethodSig(method.getModifiers(), method.getName(), method.getDeclaringClass(), method.getParameterTypes(),
                               null, method.getExceptionTypes(), method.getReturnType());
    }
}
