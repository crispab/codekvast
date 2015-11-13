package se.crisp.codekvast.agent.daemon.codebase;

import com.google.common.base.Predicate;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a Reflections filter that rejects everything, but remembers the names of "our" classes, i.e.,
 * classes with correct package prefixes.
 *
 * After doing {@code new Reflections(... new RecordingClassFileFilter(prefixes))} one can retrieve the
 * matched set of class names from the filter.
 *
 * @author olle.hallin@crisp.se
 */
class RecordingClassFileFilter implements Predicate<String> {
    private final Pattern pattern;
    private final Set<String> matches = new HashSet<String>();

    RecordingClassFileFilter(Set<String> packagePrefixes) {
        this.pattern = buildPattern(packagePrefixes);
    }

    @Override
    public boolean apply(String input) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.matches()) {
            matches.add(matcher.group(1));
        }
        return false;
    }

    public Set<String> getMatchedClassNames() {
        return new HashSet<String>(matches);
    }

    private Pattern buildPattern(Set<String> prefixes) {
        StringBuilder sb = new StringBuilder("^(");
        String delimiter = prefixes.isEmpty() ? "" : "(";
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
