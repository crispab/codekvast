/**
 * Copyright (c) 2015 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.crisp.codekvast.agent.lib.util;

import lombok.experimental.UtilityClass;
import org.aspectj.lang.Signature;
import org.aspectj.runtime.reflect.Factory;
import se.crisp.codekvast.agent.lib.config.MethodFilter;
import se.crisp.codekvast.agent.lib.model.MethodSignature;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Utility class for dealing with signatures.
 *
 * @author olle.hallin@crisp.se
 */
@UtilityClass
public class SignatureUtils {

    public static final String PUBLIC = "public";
    public static final String PROTECTED = "protected";
    public static final String PACKAGE_PRIVATE = "package-private";
    public static final String PRIVATE = "private";
    public static final String[] VISIBILITY_KEYWORDS = {PUBLIC, PROTECTED, PRIVATE};

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
        String modifiers = signature.substring(0, pos);
        return getVisibility(modifiers) + signature.substring(pos);
    }

    private static String getVisibility(String modifiers) {
        for (String v : VISIBILITY_KEYWORDS) {
            if (modifiers.contains(v)) {
                return v;
            }
        }
        return PACKAGE_PRIVATE;
    }


    /**
     * Uses AspectJ for creating the same signature as AbstractCodekvastAspect.
     *
     * @param methodFilter A filter for which methods should be included
     * @param clazz        The class containing the method
     * @param method       The method to make a signature of
     * @return The same signature object as an AspectJ execution pointcut will provide in JoinPoint.getSignature(). Returns null unless the
     * method passes the methodVisibilityFilter.
     */
    public static Signature makeSignature(MethodFilter methodFilter, Class clazz, Method method) {

        if (clazz == null || !methodFilter.shouldInclude(method)) {
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
     * Converts a java.lang.reflect.Method to a MethodSignature object.
     *
     * @param methodFilter The method visibility filter
     * @param clazz        The class containing the method
     * @param method       The method to make a signature of
     * @return A MethodSignature or null if the methodFilter stops the method.
     * @see #makeSignature(MethodFilter, Class, java.lang.reflect.Method)
     */
    public static MethodSignature makeMethodSignature(MethodFilter methodFilter, Class<?> clazz, Method method) {
        org.aspectj.lang.reflect.MethodSignature aspectjSignature =
                (org.aspectj.lang.reflect.MethodSignature) makeSignature(methodFilter, clazz, method);

        if (aspectjSignature == null) {
            return null;
        }

        MethodSignature methodSignature = MethodSignature.builder()
                                                         .aspectjString(signatureToString(aspectjSignature, true))
                                                         .declaringType(aspectjSignature.getDeclaringTypeName())
                                                         .exceptionTypes(classArrayToString(aspectjSignature.getExceptionTypes()))
                                                         .methodName(aspectjSignature.getName())
                                                         .modifiers(Modifier.toString(aspectjSignature.getModifiers()))
                                                         .packageName(aspectjSignature.getDeclaringType().getPackage().getName())
                                                         .parameterTypes(classArrayToString(aspectjSignature.getParameterTypes()))
                                                         .returnType(aspectjSignature.getReturnType().getName())
                                                         .build();

        // DEBUG
        if (methodSignature.getPackageName().isEmpty()) {
            int i = 17;
        }

        return methodSignature;

    }

    private static String classArrayToString(Class[] classes) {
        StringBuilder sb = new StringBuilder();
        String delimiter = "";

        for (Class clazz : classes) {
            sb.append(delimiter).append(clazz.getName());
            delimiter = ", ";
        }

        return sb.toString();
    }
}
