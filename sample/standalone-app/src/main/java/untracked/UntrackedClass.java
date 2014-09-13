package untracked;

/**
 * This class is outside of the packagePrefix that the CodeKvast agent monitors.
 *
 * @author Olle Hallin
 */
public class UntrackedClass {

    private int count;

    public int foo() {
        return count++;
    }
}
