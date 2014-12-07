package se.crisp.codekvast.agent.main.codebase;

import com.google.common.base.Predicate;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Olle Hallin
 */
class RecordingClassFileFilter implements Predicate<String> {
    private final Pattern pattern;
    private final Set<String> matches = new HashSet<>();

    RecordingClassFileFilter(Set<String> prefixes) {
        this.pattern = buildPattern(prefixes);
    }

    @Override
    public boolean apply(String input) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.matches()) {
            matches.add(matcher.group(1));
            return true;
        }
        return false;
    }

    public Set<String> getMatchedClassNames() {
        return new HashSet<>(matches);
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
