package duck.spike.util;

import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.runtime.reflect.Factory;

import java.lang.reflect.Method;

/**
 * @author Olle Hallin
 */
public class AspectjUtils {
    public static String makeMethodKey(Signature signature) {
        return signature.toLongString();
    }

    public static MethodSignature getMethodSignature(Class<?> clazz, Method method) {
        // Use AspectJ for creating the same signature as AbstractDuckAspect...
        return new Factory(null, clazz)
                .makeMethodSig(method.getModifiers(), method.getName(), method.getDeclaringClass(), method.getParameterTypes(),
                               null, method.getExceptionTypes(), method.getReturnType());
    }
}
