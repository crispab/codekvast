package sample.app;

/**
 * This class is inside of the packagePrefix that the Codekvast agent monitors.
 *
 * @author olle.hallin@crisp.se
 */
public class TrackedClass {
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int publicMethod() {
        return count++;
    }

    protected int protectedMethod() {
        return count++;
    }

    private int privateMethod() {
        return count++;
    }

    int packagePrivateMethod() {
        return count++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TrackedClass that = (TrackedClass) o;

        return count == that.count;

    }

    @Override
    public int hashCode() {
        return count;
    }

}
