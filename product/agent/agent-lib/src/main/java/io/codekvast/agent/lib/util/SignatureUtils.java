/*
 * Copyright (c) 2015-2017 Crisp AB
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
package io.codekvast.agent.lib.util;

import lombok.experimental.UtilityClass;
import org.aspectj.lang.Signature;
import org.aspectj.runtime.reflect.Factory;
import io.codekvast.agent.lib.model.MethodSignature;

import java.lang.reflect.Constructor;
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
    private static final String[] VISIBILITY_KEYWORDS = {PUBLIC, PROTECTED, PRIVATE};

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

    static String stripModifiersAndReturnType(String signature) {
        // Search backwards from the '(' for a space character...
        int pos = signature.indexOf("(");
        if (pos < 0) {
            // Constructor
            pos = signature.length();
        }
        while (pos >= 0 && signature.charAt(pos) != ' ') {
            pos -= 1;
        }
        String separator = pos < 0 ? " " : "";
        if (pos < 0) {
            pos = 0;
        }
        String modifiers = signature.substring(0, pos);
        return getVisibility(modifiers) + separator + signature.substring(pos);
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
     * @param clazz  The class containing the method
     * @param method The method to make a signature of
     * @return The same signature object as an AspectJ execution pointcut will provide in JoinPoint.getSignature(). Returns null unless the
     * method is not synthetic.
     */
    public static Signature makeSignature(Class clazz, Method method) {

        if (clazz == null || method.isSynthetic()) {
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
     * Uses AspectJ for creating the same signature as AbstractCodekvastAspect.
     *
     * @param clazz       The class containing the method
     * @param constructor The constructor to make a signature of
     * @return The same signature object as an AspectJ execution pointcut will provide in JoinPoint.getSignature(). Returns null unless the
     * constructor is not synthetic.
     */
    private static Signature makeSignature(Class clazz, Constructor constructor) {
        if (clazz == null || constructor.isSynthetic()) {
            return null;
        }

        return new Factory(null, clazz).makeConstructorSig(constructor.getModifiers(),
                                                           clazz,
                                                           constructor.getParameterTypes(),
                                                           null,
                                                           constructor.getExceptionTypes());
    }

    /**
     * Converts a java.lang.reflect.Method to a MethodSignature object.
     *
     * @param clazz  The class containing the method
     * @param method The method to make a signature of
     * @return A MethodSignature or null if the methodFilter stops the method.
     * @see #makeSignature(Class, Method)
     */
    public static MethodSignature makeMethodSignature(Class<?> clazz, Method method) {
        org.aspectj.lang.reflect.MethodSignature aspectjSignature =
                (org.aspectj.lang.reflect.MethodSignature) makeSignature(clazz, method);

        if (aspectjSignature == null) {
            return null;
        }

        return MethodSignature.builder()
                              .aspectjString(signatureToString(aspectjSignature, true))
                              .declaringType(aspectjSignature.getDeclaringTypeName())
                              .exceptionTypes(classArrayToString(aspectjSignature.getExceptionTypes()))
                              .methodName(aspectjSignature.getName())
                              .modifiers(Modifier.toString(aspectjSignature.getModifiers()))
                              .packageName(aspectjSignature.getDeclaringType().getPackage().getName())
                              .parameterTypes(classArrayToString(aspectjSignature.getParameterTypes()))
                              .returnType(aspectjSignature.getReturnType().getName())
                              .build();

    }

    /**
     * Converts a java.lang.reflect.Constructor to a MethodSignature object.
     *
     * @param clazz       The class containing the method.
     * @param constructor The constructor to make a signature of.
     * @return A MethodSignature or null if the methodFilter stops the constructor.
     * @see #makeSignature(Class, Method)
     */
    public static MethodSignature makeConstructorSignature(Class<?> clazz, Constructor constructor) {
        org.aspectj.lang.reflect.ConstructorSignature aspectjSignature =
                (org.aspectj.lang.reflect.ConstructorSignature) makeSignature(clazz, constructor);

        if (aspectjSignature == null) {
            return null;
        }

        return MethodSignature.builder()
                              .aspectjString(signatureToString(aspectjSignature, true))
                              .declaringType(aspectjSignature.getDeclaringTypeName())
                              .exceptionTypes(classArrayToString(aspectjSignature.getExceptionTypes()))
                              .methodName(aspectjSignature.getName())
                              .modifiers(Modifier.toString(aspectjSignature.getModifiers()))
                              .packageName(aspectjSignature.getDeclaringType().getPackage().getName())
                              .parameterTypes(classArrayToString(aspectjSignature.getParameterTypes()))
                              .returnType("")
                              .build();

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
