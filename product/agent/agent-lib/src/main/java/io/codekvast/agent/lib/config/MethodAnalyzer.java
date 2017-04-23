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
package io.codekvast.agent.lib.config;

import io.codekvast.agent.lib.util.SignatureUtils;
import lombok.EqualsAndHashCode;
import io.codekvast.agent.lib.model.v1.SignatureStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author olle.hallin@crisp.se
 */
@EqualsAndHashCode
public class MethodAnalyzer {

    private static final int VISIBILITY_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    // The missing modifier
    private static final int PACKAGE_PRIVATE = 0x08;

    private final int mask;
    private final boolean includeGetters = false;
    private final boolean includeSetters = false;
    private final boolean includeEqualsAndHashcode = false;

    public MethodAnalyzer(String visibility) {
        mask = parseVisibility(visibility);
    }

    private int parseVisibility(String visibility) {
        boolean _public = false;
        boolean _protected = false;
        boolean _private = false;
        boolean _packagePrivate = false;
        boolean recognized = false;

        String value = visibility == null ? SignatureUtils.PUBLIC : visibility.trim().toLowerCase();
        if (value.equals(SignatureUtils.PUBLIC)) {
            recognized = true;
            _public = true;
        }
        if (value.equals(SignatureUtils.PROTECTED)) {
            recognized = true;
            _public = true;
            _protected = true;
        }

        if (value.equals(SignatureUtils.PACKAGE_PRIVATE) || value.equals("!private")) {
            recognized = true;
            _public = true;
            _protected = true;
            _packagePrivate = true;
        }

        if (value.equals(SignatureUtils.PRIVATE) || value.equals("all")) {
            recognized = true;
            _public = true;
            _protected = true;
            _packagePrivate = true;
            _private = true;
        }

        if (!recognized) {
            if (!value.isEmpty()) {
                //noinspection UseOfSystemOutOrSystemErr
                System.err.println("Unrecognized value for methodVisibility: \"" + value + "\", assuming \"public\"");
            }
            _public = true;
        }

        int result = 0;
        if (_public) {
            result |= Modifier.PUBLIC;
        }
        if (_protected) {
            result |= Modifier.PROTECTED;
        }
        if (_packagePrivate) {
            result |= PACKAGE_PRIVATE;
        }
        if (_private) {
            result |= Modifier.PRIVATE;
        }
        return result;
    }

    public boolean selectsPublicMethods() {
        return (mask & Modifier.PUBLIC) != 0;
    }

    public boolean selectsProtectedMethods() {
        return (mask & Modifier.PROTECTED) != 0;
    }

    public boolean selectsPackagePrivateMethods() {
        return (mask & PACKAGE_PRIVATE) != 0;
    }

    public boolean selectsPrivateMethods() {
        return (mask & Modifier.PRIVATE) != 0;
    }

    public SignatureStatus apply(Method method) {
        if (!shouldIncludeByModifiers(method.getModifiers())) {
            return SignatureStatus.EXCLUDED_BY_VISIBILITY;
        }
        if (isGetter(method)
                || isSetter(method)
                || isEquals(method)
                || isHashCode(method)) {
            return SignatureStatus.EXCLUDED_SINCE_TRIVIAL;
        }
        return SignatureStatus.NOT_INVOKED;
    }

    public SignatureStatus apply(Constructor constructor) {
        if (!shouldIncludeByModifiers(constructor.getModifiers())) {
            return SignatureStatus.EXCLUDED_BY_VISIBILITY;
        }
        return SignatureStatus.NOT_INVOKED;
    }

    /**
     * Given a Method.modifiers() tell whether a method should be included in the inventory or not.
     *
     * @param modifiers Returned from {@link java.lang.reflect.Method#getModifiers()}
     * @return True if any of the visibility bits in modifiers matches this object.
     */
    boolean shouldIncludeByModifiers(int modifiers) {
        // Package private is an anomaly, since it is the lack of any visibility modifier.
        if (selectsPackagePrivateMethods() && (modifiers & VISIBILITY_MODIFIERS) == 0) {
            return true;
        }
        // At least one of the visibility bits match
        return (modifiers & mask) != 0;
    }

    boolean isEquals(Method method) {
        return !isStatic(method)
                && method.getName().equals("equals")
                && method.getParameterTypes().length == 1
                && method.getReturnType().equals(Boolean.TYPE);
    }

    boolean isHashCode(Method method) {
        return !isStatic(method)
                && method.getName().equals("hashCode")
                && method.getParameterTypes().length == 0
                && method.getReturnType().equals(Integer.TYPE);
    }

    boolean isCompareTo(Method m) {
        return !isStatic(m)
                && m.getName().equals("compareTo")
                && m.getParameterTypes().length == 1
                && m.getReturnType().equals(Integer.TYPE);
    }

    boolean isToString(Method method) {
        return !isStatic(method)
                && method.getName().equals("toString")
                && method.getParameterTypes().length == 0
                && method.getReturnType().equals(String.class);
    }

    boolean isSetter(Method method) {
        return !isStatic(method)
                && method.getName().startsWith("set")
                && method.getParameterTypes().length == 1
                && method.getReturnType().equals(Void.TYPE);
    }

    boolean isGetter(Method method) {
        return !isStatic(method)
                && method.getName().startsWith("get")
                && method.getParameterTypes().length == 0
                && !method.getReturnType().equals(Void.TYPE);
    }

    private boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    @Override
    public String toString() {
        if (selectsPrivateMethods()) {
            return SignatureUtils.PRIVATE;
        }
        if (selectsPackagePrivateMethods()) {
            return SignatureUtils.PACKAGE_PRIVATE;
        }
        if (selectsProtectedMethods()) {
            return SignatureUtils.PROTECTED;
        }
        return SignatureUtils.PUBLIC;
    }

}
