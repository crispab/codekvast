package se.crisp.codekvast.agent.main.appversion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collection;

/**
 * A strategy for literal versions. Handles "literal v" and "constant v".
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Slf4j
@Component
public class LiteralAppVersionStrategy extends AbstractAppVersionStrategy {

    public LiteralAppVersionStrategy() {
        super("constant", "literal");
    }

    @Override
    public boolean canHandle(String[] args) {
        return args != null && args.length == 2 && recognizes(args[0]);
    }

    @Override
    public String resolveAppVersion(Collection<File> codeBases, String[] args) {
        return args[1].trim();
    }
}
