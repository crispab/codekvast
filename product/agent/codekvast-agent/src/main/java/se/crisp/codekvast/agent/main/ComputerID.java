package se.crisp.codekvast.agent.main;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * This is a UUID in disguise. It support creating a random UUID and stuff it into java.util.prefs.Preferences so that it remains invariant
 * on the same computer.
 * <p/>
 * The reason for wrapping the UUID is that it should be simpler to {@literal @Inject}
 *
 * @author Olle Hallin
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ComputerID {
    private static final String PREFS_NODE_PATH = "se/crisp/codekvast/agent";
    private static final String PREFS_AGENT_ID_KEY = "agentId";

    private final String value;

    @Override
    public String toString() {
        return value;
    }

    public static ComputerID get() {

        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_PATH);
        String value = preferences.get(PREFS_AGENT_ID_KEY, null);
        if (value == null) {
            value = UUID.randomUUID().toString();
            log.info("Generated the computer ID {}", value);
            preferences.put(PREFS_AGENT_ID_KEY, value);
        } else {
            log.debug("Retrieved the computer ID {}", value);
        }
        return new ComputerID(value);
    }
}
