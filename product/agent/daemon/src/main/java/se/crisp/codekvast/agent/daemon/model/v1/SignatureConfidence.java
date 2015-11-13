/*
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

package se.crisp.codekvast.agent.daemon.model.v1;

/**
 * @author olle.hallin@crisp.se
 */
public enum SignatureConfidence {
    /**
     * The used signature was found as-is in the scanned code base.
     */
    EXACT_MATCH,

    /**
     * The used signature was <em>not</em> found as-is in the scanned code base. It was found however, when searching upwards in the class
     * hierarchy. The reason for not finding it in the first place could be that the method was synthesized at runtime by some byte code
     * manipulating AOP framework (like Spring or Guice).
     */
    FOUND_IN_PARENT_CLASS,

    /**
     * The used signature was <em>not</em> found at all in the scanned code base. This indicates a problem with the code base scanner.
     * Access to the source code is required in order to resolve the problem.
     */
    NOT_FOUND_IN_CODE_BASE;

    /**
     * Converts a SignatureConfidence.ordinal() back to the enum constant.
     *
     * @param ordinal An Integer returned by SignatureConfidence.ordinal(). May be null.
     * @return The proper enum constant or null if {@code ordinal} is null.
     * @throws IllegalArgumentException If invalid ordinal value other than null.
     */
    public static SignatureConfidence fromOrdinal(Integer ordinal) {
        if (ordinal == null) {
            return null;
        }
        for (SignatureConfidence confidence : SignatureConfidence.values()) {
            if (confidence.ordinal() == ordinal) {
                return confidence;
            }
        }
        throw new IllegalArgumentException("Unknown " + SignatureConfidence.class.getSimpleName() + " ordinal: " + ordinal);
    }
}
