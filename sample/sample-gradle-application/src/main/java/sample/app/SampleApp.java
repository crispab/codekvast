package sample.app;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.bridge.SLF4JBridgeHandler;
import sample.lib.used.Bar1;
import sample.lib.used.Bar2;
import untracked.UntrackedClass;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
@Slf4j
public class SampleApp {

    private int sum = 0;

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public static void main(String[] args) {
        new SampleApp().run();
    }

    @SneakyThrows(InterruptedException.class)
    private void run() {
        System.out.printf("Hello, World! from %s%n%n", getClass().getName());
        tryToLoadClass("io.codekvast.javaagent.CodekvastAgent", true);
        tryToLoadClass("org.aspectj.weaver.loadtime.Agent", false);

        measureMethodCallTrackingOverhead();

        Thread.sleep(4000L);
        new Bar1().declaredOnBar();

        Thread.sleep(4000L);
        new Bar1().declaredOnBar();

        Thread.sleep(4000L);
        new Bar1().declaredOnBar();
        new Bar2().declaredOnBar();
        new Bar2().declaredOnBar2();
    }

    private void measureMethodCallTrackingOverhead() {
        System.out.println("\nMeasuring method call tracking overhead...");

        int count = 10000000;

        // warm-up
        invokeTracked(count);
        invokeUntracked(count);
        invokeUntrackedLogged(count);

        long untrackedNotLoggedElapsedMillis = invokeUntracked(count);
        long untrackedLoggedElapsedMillis = invokeUntrackedLogged(count);
        long trackedElapsedMillis = invokeTracked(count);

        int overheadNanosUntrackedLoggedComparedToUntrackedUnlogged = (int) ((untrackedLoggedElapsedMillis - untrackedNotLoggedElapsedMillis) * 1000000d / (double) count);
        int overheadNanosTrackedComparedToUntracked = (int) ((trackedElapsedMillis - untrackedNotLoggedElapsedMillis) * 1000000d / (double) count);
        int overheadNanosTrackedComparedToLoggerCall =
            (int) ((trackedElapsedMillis - untrackedLoggedElapsedMillis - untrackedNotLoggedElapsedMillis) * 1000000d / (double) count);

        System.out.println();
        System.out.printf("Invoked a trivial untracked          method %,d times in %5d ms%n", count, untrackedNotLoggedElapsedMillis);
        System.out.printf("Invoked a trivial untracked   logged method %,d times in %5d ms%n", count, untrackedLoggedElapsedMillis);
        System.out.printf("Invoked a trivial   tracked unlogged method %,d times in %5d ms%n", count, trackedElapsedMillis);
        System.out.println();
        System.out.printf("Calling a disabled logger adds roughly %d ns compared to a plain method call%n", overheadNanosUntrackedLoggedComparedToUntrackedUnlogged);
        System.out.printf("Codekvast                 adds roughly %d ns compared to a plain method call%n", overheadNanosTrackedComparedToUntracked);
        System.out.printf("Codekvast                 adds roughly %d ns compared to a logger call%n", overheadNanosTrackedComparedToLoggerCall);
        System.out.println();

        if (overheadNanosTrackedComparedToUntracked <= 5) {
            throw new IllegalStateException(
                "Unreasonable overhead: " + overheadNanosTrackedComparedToUntracked + ". Is Codekvast Agent correctly wired?");
        }
    }

    private long invokeUntracked(int count) {
        long startedAt = System.currentTimeMillis();
        sum = 0;
        UntrackedClass untracked = new UntrackedClass();
        for (int i = 0; i < count; i++) {
            sum += untracked.foo();
        }
        logger.debug("Make sure the entire loop is not bypassed by the JIT compiler... {}", sum);
        return System.currentTimeMillis() - startedAt;
    }

    private long invokeUntrackedLogged(int count) {
        long startedAt = System.currentTimeMillis();
        sum = 0;
        UntrackedClass untracked = new UntrackedClass();
        for (int i = 0; i < count; i++) {
            sum += untracked.fooLogged();
        }
        logger.debug("Make sure the entire loop is not bypassed by the JIT compiler... {}", sum);
        return System.currentTimeMillis() - startedAt;
    }

    private long invokeTracked(int count) {
        long startedAt = System.currentTimeMillis();

        sum = 0;
        TrackedClass tracked = new TrackedClass();
        for (int i = 0; i < count; i++) {
            sum += tracked.publicMethod();
        }
        logger.debug("Make sure the entire loop is not bypassed by the JIT compiler... {}", sum);
        return System.currentTimeMillis() - startedAt;
    }

    private void tryToLoadClass(String className, boolean expectedSuccess) {
        try {
            Class.forName(className);

            String result = expectedSuccess ? "GOOD" : "BAD";
            System.out.printf("%s: %s can load class %s%n", result, SampleApp.class.getName(), className);
        } catch (ClassNotFoundException e) {
            String result = expectedSuccess ? "BAD" : "GOOD";
            System.out.printf("%s: %s cannot load class %s%n", result, SampleApp.class.getName(), className);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public String meaningOfLife() {
        return "42";
    }

}
