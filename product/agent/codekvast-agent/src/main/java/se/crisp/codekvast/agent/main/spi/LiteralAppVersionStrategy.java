package se.crisp.codekvast.agent.main.spi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A strategy for literal versions. Handles "literal v" and "constant v".
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Slf4j
@Component
public class LiteralAppVersionStrategy implements AppVersionStrategy {

    private final Set<String> MY_NAMES = new HashSet<>(Arrays.asList("constant", "literal"));

    @Override
    public boolean canHandle(String[] args) {
        return args != null && args.length == 2 && recognizes(args[0]);
    }

    private boolean recognizes(String name) {
        return MY_NAMES.contains(name.toLowerCase().trim());
    }

    @Override
    public String resolveAppVersion(String[] args) {
        return args[1].trim();
    }
}
