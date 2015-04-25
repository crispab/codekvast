package sample.app;

/**
 * @author olle.hallin@crisp.se
 */
public abstract class AbstractTracked {

    protected int count;

    protected int protectedMethod() {
        return count++;
    }
}
