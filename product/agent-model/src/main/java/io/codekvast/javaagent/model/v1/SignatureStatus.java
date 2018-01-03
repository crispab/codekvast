/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
package io.codekvast.javaagent.model.v1;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * The status of a method signature.
 *
 * NOTE: Is also defined as an ENUM in the central dashboard's invocations table!
 *
 * @author olle.hallin@crisp.se
 * @deprecated Use {@link io.codekvast.javaagent.model.v2.SignatureStatus2} instead.
 */
@Getter
@RequiredArgsConstructor
@Deprecated
public enum SignatureStatus implements Serializable {
    /**
     * The signature has been detected in the codebase, but it has never been invoked.
     */
    NOT_INVOKED(1, true),

    /**
     * The invoked signature was found as-is in the scanned code base.
     */
    INVOKED(2, true),

    /**
     * The invoked signature was <em>not</em> found as-is in the scanned code base. It was found however, when searching upwards in the
     * class hierarchy. The reason for not finding it in the first place could be that the method was synthesized at runtime by some byte
     * code manipulating AOP framework like Spring or Guice.
     */
    FOUND_IN_PARENT_CLASS(3, true),

    /**
     * The invoked signature was <em>not</em> found at all in the scanned code base. This indicates a problem with the code base scanner.
     * Access to the scanned application's source code is required in order to resolve the problem.
     */
    NOT_FOUND_IN_CODE_BASE(4, true),

    /**
     * The signature was found in the code base but was excluded from being tracked since it belongs to an excluded package.
     */
    EXCLUDED_BY_PACKAGE_NAME(5, false),

    /**
     * The signature was found in the code base but was excluded from being tracked since it has wrong visibility.
     */
    EXCLUDED_BY_VISIBILITY(6, false),

    /**
     * The signature was found in the code base but was excluded from being tracked since it is a trivial method (setter, getter, equals(),
     * hashCode(), toString() etc).
     */
    EXCLUDED_SINCE_TRIVIAL(7, false);

    /*
     * The database representation of the value. It is 1-based to make it easy to use with MariaDB's ENUM column type.
     * MariaDB reserves 0 for the special value ''.
     */
    private final int dbNumber;

    /**
     * Is the method tracked by Codekvast?
     */
    private final boolean tracked;

}
