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

    private static void tryToLoadClass(String className, boolean shouldBeAvailable) {
        try {
            Class.forName(className);

            String verdict = shouldBeAvailable ? "GOOD:" : "BAD: ";
            String result = shouldBeAvailable
                    ? "is unavoidable."
                    : "has leaked into my class path from -javaagent:duck-agent.jar";
            System.out.printf("%s %s can load class %s, which %s%n", verdict, SampleApp.class.getName(), className, result);
        } catch (ClassNotFoundException e) {

            String verdict = shouldBeAvailable ? "BAD: " : "GOOD:";
            String result = shouldBeAvailable
                    ? ", which indicates that duck-agent is not enabled!"
                    : ". We don't want duck-agent internals to leak into the application.";
            System.out.printf("%s %s cannot load class %s%s%n", verdict, SampleApp.class.getName(), className, result);
        }
    }

    public String meaningOfLife() {
        return "42";
    }

}
