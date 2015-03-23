package sample.app;

/**
 * This class is inside of the packagePrefix that the Codekvast agent monitors.
 *
 * @author olle.hallin@crisp.se
 */
public class TrackedClass {

    private int count;

    public int publicMethod() {
        return count++;
    }

    protected int protectedMethod() {
        return count++;
    }

    private int privateMethod() {
        return count++;
    }

    int moduleMethod() {
        return count++;
    }
}
