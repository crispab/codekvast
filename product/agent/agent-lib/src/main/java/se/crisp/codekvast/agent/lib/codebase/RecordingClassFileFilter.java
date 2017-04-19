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
package se.crisp.codekvast.agent.lib.codebase;

import com.google.common.base.Predicate;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a Reflections filter that rejects everything, but remembers the names of "our" classes, i.e.,
 * classes with correct package name prefixes.
 *
 * After doing {@code new Reflections(... new RecordingClassFileFilter(prefixes))} one can retrieve the
 * matched set of class names from the filter.
 *
 * @author olle.hallin@crisp.se
 */
class RecordingClassFileFilter implements Predicate<String> {
    private final Pattern includePattern;
    private final Set<String> matches = new HashSet<String>();

    RecordingClassFileFilter(Set<String> packages) {
        this.includePattern = buildPattern(packages);
    }

    @Override
    public boolean apply(String input) {
        if (includePattern != null) {
            Matcher includeMatcher = includePattern.matcher(input);
            if (includeMatcher.matches()) {
                matches.add(includeMatcher.group(1));
            }
        }
        return false;
    }

    Set<String> getMatchedClassNames() {
        return new HashSet<String>(matches);
    }

    private Pattern buildPattern(Set<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("^(");
        String delimiter = "(";
        for (String prefix : prefixes) {
            sb.append(delimiter).append(prefix);
            delimiter = "|";
        }
        if (!prefixes.isEmpty()) {
            sb.append(")");
        }
        sb.append("\\..*)\\.class$");
        return Pattern.compile(sb.toString());
    }

}
