package se.crisp.codekvast.agent.util;

import org.aspectj.lang.Signature;
import org.aspectj.runtime.reflect.Factory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Utility class for dealing with signatures.
 *
 * @author Olle Hallin
 */
public class SignatureUtils {
    private SignatureUtils() {
        // utility class
    }

    /**
     * Converts a method signature to a string containing the bare minimum to uniquely identify the method, namely: <ul>
     * <li>The declaring class name</li> <li>The method name</li> <li>The full parameter types</li> </ul>
     */
    public static String signatureToString(Signature signature) {
        if (signature == null) {
            return null;
        }
        String s = signature.toLongString();

        // Search backwards from the '(' for a space character...
        int pos = s.indexOf("(");
        while (pos >= 0 && s.charAt(pos) != ' ') {
            pos -= 1;
        }
        return s.substring(pos + 1);
    }

    /**
     * Uses AspectJ for creating the same signature as AbstractCodekvastAspect.
     *
     * @return The same signature object as an AspectJ execution pointcut will provide in JoinPoint.getSignature(). Returns null unless the
     * method is public.
     */
    public static Signature makeSignature(Class clazz, Method method) {

        if (clazz == null || !Modifier.isPublic(method.getModifiers())) {
            return null;
        }

        return new Factory(null, clazz).makeMethodSig(method.getModifiers(),
                                                      method.getName(),
                                                      clazz,
                                                      method.getParameterTypes(),
                                                      null,
                                                      method.getExceptionTypes(),
                                                      method.getReturnType());
    }

    /**
     * Convenience method.
     *
     * @see #makeSignature(Class, java.lang.reflect.Method)
     * @see #signatureToString(org.aspectj.lang.Signature)
     */
    public static String makeSignatureString(Class<?> clazz, Method method) {
        return signatureToString(makeSignature(clazz, method));
    }
}
