package sample.app;

/**
 * This class is outside of the packagePrefix that the Codekvast agent monitors.
 *
 * @author olle.hallin@crisp.se
 */
public class TrackedClass {

    private int count;

    public int foo() {
        return count++;
    }
}
