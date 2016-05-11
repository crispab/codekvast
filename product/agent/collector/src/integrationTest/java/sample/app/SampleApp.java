package sample.app;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("ALL")
public class SampleApp {
    private final int dummy;

    public SampleApp() {
        dummy = 17;
    }

    public int add(int p1, int p2) {
        return p1 + p2;
    }

    public static void main(String[] args) {
        System.out.println("2+2=" + new SampleApp().add(2, 2));
    }
}
