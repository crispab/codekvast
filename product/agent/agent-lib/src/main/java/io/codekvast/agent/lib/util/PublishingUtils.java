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
import lombok.extern.slf4j.Slf4j;

/**
 * @author olle.hallin@crisp.se
 */
@UtilityClass
@Slf4j
public class PublishingUtils {

    /**
     * Removes all modifiers from a signature.
     *
     * Modifiers are everything before the fully qualified class name.
     *
     * @param signature The signature to strip.
     * @return A plain signature with all text before the fully qualified class name removed.
     */
    public static String stripModifiers(String signature) {
        int pos = signature.indexOf('(');
        if (pos < 0) {
            // Constructor
            pos = signature.length() - 1;
        }
        while (pos >= 0 && signature.charAt(pos) != ' ') {
            pos -= 1;
        }
        String result = signature.substring(pos + 1);
        if (result.indexOf(')') >= 0 && result.indexOf('(') < 0) {
            throw new IllegalStateException(String.format("Unbalanced parenthesis in '%s': '%s'", signature, result));

        }
        if (!isValid(result)) {
            throw new IllegalStateException(String.format("Failed to strip modifiers in '%s': result='%s", signature, result));
        }
        return result;
    }

    public static boolean isValid(String signature) {
        int lparen = signature.indexOf('(');
        int rparen = signature.indexOf(')');
        if (((lparen < 0) && (rparen >= 0)) || ((lparen >= 0) && (rparen < 0)) || (lparen > rparen)) {
            log.error("Invalid signature in '{}'", signature);
            return false;
        }
        return true;
    }


}
