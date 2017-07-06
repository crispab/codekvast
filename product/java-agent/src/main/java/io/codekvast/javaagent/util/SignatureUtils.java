/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
package io.codekvast.javaagent.util;

import io.codekvast.javaagent.model.v1.MethodSignature;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.aspectj.lang.Signature;
import org.aspectj.runtime.reflect.Factory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utility class for dealing with signatures.
 *
 * @author olle.hallin@crisp.se
 */
@UtilityClass
@Log
public class SignatureUtils {

    private static final String ADDED_PATTERNS_FILENAME = "/io/codekvast/byte-code-added-methods.txt";
    private static final String ENHANCED_PATTERNS_FILENAME = "/io/codekvast/byte-code-enhanced-methods.txt";

    public static final String PUBLIC = "public";
    public static final String PROTECTED = "protected";
    public static final String PACKAGE_PRIVATE = "package-private";
    public static final String PRIVATE = "private";
    private static final String[] VISIBILITY_KEYWORDS = {PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE};

    private static final List<Pattern> bytecodeAddedPatterns;
    private static final List<Pattern> bytecodeEnhancedPatterns;
    private static final Set<Pattern> loggedBadPatterns = new HashSet<>();
    private static final Set<String> strangeSignatures = new TreeSet<>();

    static {
        bytecodeAddedPatterns = readByteCodePatternsFrom(ADDED_PATTERNS_FILENAME);
        bytecodeEnhancedPatterns = readByteCodePatternsFrom(ENHANCED_PATTERNS_FILENAME);
    }

    private static List<Pattern> readByteCodePatternsFrom(String resourceName) {
        List<Pattern> result = new ArrayList<>();
        logger.finer("Reading byte code patterns from " + resourceName);
        try {
            LineNumberReader reader = new LineNumberReader(
                new BufferedReader(
                    new InputStreamReader(SignatureUtils.class.getResource(resourceName).openStream(), Charset.forName("UTF-8"))));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    addPatternTo(result, resourceName, reader.getLineNumber(), line);
                }
            }
        } catch (Exception e) {
            logger.severe("Cannot read " + resourceName);
        }
        return result;
    }

    private static void addPatternTo(Collection<Pattern> result, String fileName, int lineNumber, String pattern) {
        try {
            result.add(Pattern.compile(pattern));
        } catch (PatternSyntaxException e) {
            logger.severe(String.format("Illegal regexp syntax in %s:%s: %s", fileName, lineNumber, e.toString()));
        }
    }

    public static String normalizeSignature(MethodSignature methodSignature) {
        return methodSignature == null ? null : normalizeSignature(methodSignature.getAspectjString());
    }

    public static String normalizeSignature(String signature) {
        if (signature == null) {
            return null;
        }

        if (isStrangeSignature(signature)) {
            strangeSignatures.add(signature);
        }

        for (Pattern pattern : bytecodeAddedPatterns) {
            if (pattern.matcher(signature).matches()) {
                return null;
            }
        }
        String result = signature.replaceAll(" final ", " ");

        for (Pattern pattern : bytecodeEnhancedPatterns) {
            Matcher matcher = pattern.matcher(result);
            if (matcher.matches()) {
                if (matcher.groupCount() != 3) {
                    logBadPattern(pattern);
                } else {
                    result = matcher.group(1) + "." + matcher.group(2) + matcher.group(3);
                    logger.finer(String.format("Normalized %s to %s", signature, result));
                    break;
                }
            }
        }

        if (isStrangeSignature(result)) {
            logger.warning(String.format("Could not normalize %s: %s", signature, result));
        }
        return result;
    }

    boolean isStrangeSignature(String signature) {
        return signature.contains("..") || signature.contains("$$") || signature.contains("CGLIB")
            || signature.contains("EnhancerByGuice") || signature.contains("FastClassByGuice");
    }

    private void logBadPattern(Pattern pattern) {
        if (loggedBadPatterns.add(pattern)) {
            logger.severe(String.format("Expected exactly 3 capturing groups in regexp '%s', ignored.", pattern));
        }
    }


    /**
     * Converts a (method) signature to a string containing the bare minimum to uniquely identify the method, namely: <ul> <li>The declaring
     * class name</li> <li>The method name</li> <li>The full parameter types</li> </ul>
     *
     * @param signature The signature to convert.
     * @return A string representation of the signature.
     */
    public static String signatureToString(Signature signature) {
        return signature == null ? null : signature.toLongString();
    }

    public static String stripModifiers(String signature) {
        // Search backwards from the '(' for a space character...
        int pos = signature.indexOf("(");
        if (pos < 0) {
            // Constructor
            pos = signature.length() - 1;
        }
        while (pos >= 0 && signature.charAt(pos) != ' ') {
            pos -= 1;
        }
        return signature.substring(pos + 1);
    }

    static String stripModifiersAndReturnType(String signature) {
        if (signature == null) {
            return null;
        }
        return getVisibility(signature) + " " + stripModifiers(signature);
    }

    public static String getVisibility(String modifiers) {
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
                              .aspectjString(stripModifiersAndReturnType(signatureToString(aspectjSignature)))
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
                              .aspectjString(stripModifiersAndReturnType(signatureToString(aspectjSignature)))
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

    public static Map<String, String> getStrangeSignatureMap() {
        Map<String, String> result = new TreeMap<>();
        for (String s : strangeSignatures) {
            result.put(s, normalizeSignature(s));
        }
        return result;
    }


}
