package sample.app;

/**
 * @author olle.hallin@crisp.se
 */
abstract class AbstractTracked {

    int count;

    protected int protectedMethod() {
        return count++;
    }
}
