package untracked;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is outside of the packagePrefix that the Codekvast agent monitors.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class UntrackedClass {

    private int count;

    public int foo() {
        return count++;
    }

    public int fooLogged() {
        log.trace("Invoked fooLogged() #{}", count);
        return count++;
    }
}
