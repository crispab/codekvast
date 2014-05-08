package se.crisp.app;

/**
 * @author Olle Hallin
 */
public class SampleApp {

    public static void main(String[] args) {
        System.out.printf("Hello, World! from %s%n%n", SampleApp.class.getName());
        new Bar().m1();

        tryToLoadClass("duck.spike.DuckAgent", true);
        tryToLoadClass("org.aspectj.weaver.loadtime.Agent", true);
        tryToLoadClass("org.reflections.Reflections", false);
    }

    private static void tryToLoadClass(String className, boolean expectedResult) {
        try {
            Class.forName(className);
            String result = expectedResult ? "unavoidable" : "BAD!";
            System.out.printf("%s can load class %s, which is %s.%n", SampleApp.class.getName(), className, result);
        } catch (ClassNotFoundException e) {
            String result = expectedResult ? "an ERROR" : "desirable";
            System.out.printf("%s cannot load class %s, which is %s.%n", SampleApp.class.getName(), className, result);
        }
    }

    public String meaningOfLife() {
        return "42";
    }

}
