package sample.app;

/**
 * @author olle.hallin@crisp.se
 */
public class SampleApp {

    public int add(int p1, int p2) {
        return p1 + p2;
    }

    public static void main(String[] args) {
        System.out.println("2+2=" + new SampleApp().add(2, 2));
    }
}
