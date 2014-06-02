package sample.app;

/**
 * This class is outside of the packagePrefix that the Duck agent monitors.
 *
 * @author Olle Hallin
 */
public class TrackedClass {

    private int count;

    public int foo() {
        return count++;
    }
}
