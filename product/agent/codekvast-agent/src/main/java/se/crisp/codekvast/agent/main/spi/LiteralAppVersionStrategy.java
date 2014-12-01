package se.crisp.codekvast.agent.main.spi;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Slf4j
public class LiteralAppVersionStrategy implements AppVersionStrategy {
    @Override
    public String getName() {
        return "literal";
    }

    @Override
    public String getAppVersion(String[] args) {
        return args[1].trim();
    }
}
