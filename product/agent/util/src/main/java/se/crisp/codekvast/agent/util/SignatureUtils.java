package se.crisp.codekvast.agent.util;

import lombok.experimental.UtilityClass;
import org.aspectj.lang.Signature;
import org.aspectj.runtime.reflect.Factory;
import se.crisp.codekvast.agent.config.MethodVisibilityFilter;

import java.lang.reflect.Method;

/**
 * Utility class for dealing with signatures.
 *
 * @author olle.hallin@crisp.se
 */
@UtilityClass
public class SignatureUtils {

    /**
     * Converts a (method) signature to a string containing the bare minimum to uniquely identify the method, namely: <ul> <li>The declaring
     * class name</li> <li>The method name</li> <li>The full parameter types</li> </ul>
     *
     * @param signature                   The signature to convert
     * @param stripModifiersAndReturnType Should we strip modifiers and return type?
     * @return A string representation of the signature, optionally minimized
     */
    public static String signatureToString(Signature signature, boolean stripModifiersAndReturnType) {
        if (signature == null) {
            return null;
        }
        String s = signature.toLongString();
        return stripModifiersAndReturnType ? stripModifiersAndReturnType(s) : s;
    }

    public static String stripModifiersAndReturnType(String signature) {
        // Search backwards from the '(' for a space character...
        int pos = signature.indexOf("(");
        while (pos >= 0 && signature.charAt(pos) != ' ') {
            pos -= 1;
        }
        return signature.substring(pos + 1);
    }


    /**
     * Uses AspectJ for creating the same signature as AbstractCodekvastAspect.
     *
     * @param clazz  The class containing the method
     * @param method The method to make a signature of
     * @return The same signature object as an AspectJ execution pointcut will provide in JoinPoint.getSignature(). Returns null unless the
     * method is public.
     */
    public static Signature makeSignature(MethodVisibilityFilter methodVisibilityFilter, Class clazz, Method method) {

        if (clazz == null || !methodVisibilityFilter.shouldInclude(method.getModifiers())) {
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
     * @param methodVisibilityFilter The method visibility filter
     * @param clazz  The class containing the method
     * @param method The method to make a signature of
     * @see #makeSignature(MethodVisibilityFilter, Class, java.lang.reflect.Method)
     * @see #signatureToString(org.aspectj.lang.Signature, boolean)
     * @return A String representation of the signature.
     */
    public static String makeSignatureString(MethodVisibilityFilter methodVisibilityFilter, Class<?> clazz, Method method) {
        return signatureToString(makeSignature(methodVisibilityFilter, clazz, method), true);
    }
}
