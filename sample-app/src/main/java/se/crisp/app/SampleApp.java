package se.crisp.app;

import untracked.UntrackedClass;

/**
 * @author Olle Hallin
 */
public class SampleApp {

    static int sum = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.printf("Hello, World! from %s%n%n", SampleApp.class.getName());

        tryToLoadClass("duck.spike.DuckAgent", true);
        tryToLoadClass("org.aspectj.weaver.loadtime.Agent", true);
        tryToLoadClass("org.reflections.Reflections", false);

        measureMethodCallTrackingOverhead();

        System.out.printf("%s sleeps 20 seconds...%n", SampleApp.class.getSimpleName());
        Thread.sleep(20000L);
        System.out.printf("%s wakes up%n", SampleApp.class.getSimpleName());

        new Bar().m1();
    }

    private static void measureMethodCallTrackingOverhead() {
        System.out.println("\nMeasuring method call tracking overhead...");

        int count = 10000000;

        // varm-up
        invokeTracked(count);
        invokeUntracked(count);

        long untrackedElapsedMillis = invokeUntracked(count);
        long trackedElapsedMillis = invokeTracked(count);
        double overheadMicros = (trackedElapsedMillis - untrackedElapsedMillis) * 1000d / (double) count;

        System.out.printf("Invoked a trivial untracked method %d times in %5d ms%n", count, untrackedElapsedMillis);
        System.out.printf("Invoked a trivial   tracked method %d times in %5d ms%n", count, trackedElapsedMillis);
        System.out.printf("Duck instrumentation adds roughly %.2f us to a method call%n", overheadMicros);
    }

    private static long invokeUntracked(int count) {
        long startedAt = System.currentTimeMillis();
        sum = 0;
        UntrackedClass untracked = new UntrackedClass();
        for (int i = 0; i < count; i++) {
            sum += untracked.foo();
        }
        return System.currentTimeMillis() - startedAt;
    }

    private static long invokeTracked(int count) {
        long startedAt = System.currentTimeMillis();

        sum = 0;
        TrackedClass tracked = new TrackedClass();
        for (int i = 0; i < count; i++) {
            sum += tracked.foo();
        }
        return System.currentTimeMillis() - startedAt;
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
