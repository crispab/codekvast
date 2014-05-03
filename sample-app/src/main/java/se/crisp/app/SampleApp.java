package se.crisp.app;

/**
 * @author Olle Hallin
 */
public class SampleApp {

    public static void main(String[] args) {
        System.out.printf("Hello, World! from %s%n", SampleApp.class.getName());
        new Bar().m1();
    }

    public String meaningOfLife() {
        return "42";
    }

}
