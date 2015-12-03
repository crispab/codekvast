package se.crisp.codekvast.agent.collector;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is an elaboratory test for measuring performance for different implementations of concurrent sets.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
@RunWith(Parameterized.class)
public class ConcurrentSetPerformanceTest {

    interface Strategy {
        void add(String s);

        void clear();

        boolean isThreadSafe();
    }

    static class ConcurrentSkipListStrategy implements Strategy {
        private final Set<String> set = new ConcurrentSkipListSet<String>();

        @Override
        public void add(String s) {
            set.add(s);
        }

        @Override
        public void clear() {
            set.clear();
        }

        @Override
        public boolean isThreadSafe() {
            return true;
        }
    }

    static class SetFromConcurrentHashMapStrategy implements Strategy {
        private final Set<String> set = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

        @Override
        public void add(String s) {
            set.add(s);
        }

        @Override
        public void clear() {
            set.clear();
        }

        @Override
        public boolean isThreadSafe() {
            return true;
        }
    }

    static class SetFromConcurrentSkipListMapStrategy implements Strategy {
        private final Set<String> set = Collections.newSetFromMap(new ConcurrentSkipListMap<String, Boolean>());

        @Override
        public void add(String s) {
            set.add(s);
        }

        @Override
        public void clear() {
            set.clear();
        }

        @Override
        public boolean isThreadSafe() {
            return true;
        }
    }

    static class ConcurrentHashMapStrategy implements Strategy {
        private final Map<String, Object> map = new ConcurrentHashMap<String, Object>();

        @Override
        public void add(String s) {
            map.put(s, Boolean.TRUE);
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public boolean isThreadSafe() {
            return true;
        }
    }

    static class ManuallySynchronizedHashSetStrategy implements Strategy {
        private final Set<String> set = new HashSet<String>();

        @Override
        public void add(String s) {
            synchronized (set) {
                set.add(s);
            }
        }

        @Override
        public void clear() {
            synchronized (set) {
                set.clear();
            }
        }

        @Override
        public boolean isThreadSafe() {
            return true;
        }
    }

    static class ReadWriteLockHashSetStrategy implements Strategy {
        private final Set<String> set = new HashSet<String>();
        private ReadWriteLock lock = new ReentrantReadWriteLock();

        @Override
        public void add(String s) {
            lock.writeLock().lock();
            try {
                set.add(s);
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public void clear() {
            lock.writeLock().lock();
            try {
                set.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public boolean isThreadSafe() {
            return true;
        }
    }

    static class WrappedSynchronizedHashSetStrategy implements Strategy {
        private final Set<String> set = Collections.synchronizedSet(new HashSet<String>());

        @Override
        public void add(String s) {
            set.add(s);
        }

        @Override
        public void clear() {
            set.clear();
        }

        @Override
        public boolean isThreadSafe() {
            return true;
        }
    }

    static class UnsynchronizedHashSetStrategy implements Strategy {
        private final Set<String> set = new HashSet<String>();

        @Override
        public void add(String s) {
            set.add(s);
        }

        @Override
        public void clear() {
            set.clear();
        }

        @Override
        public boolean isThreadSafe() {
            return false;
        }
    }

    private static final Strategy STRATEGIES[] = {
            new ConcurrentSkipListStrategy(),
            new SetFromConcurrentSkipListMapStrategy(),
            new SetFromConcurrentHashMapStrategy(),
            new ConcurrentHashMapStrategy(),
            new ManuallySynchronizedHashSetStrategy(),
            new ReadWriteLockHashSetStrategy(),
            new WrappedSynchronizedHashSetStrategy(),
            new UnsynchronizedHashSetStrategy()
    };

    private static final Random RANDOM = new Random();

    private static final String RANDOM_STRINGS[] = generateRandomStrings(100, 100);

    private static String[] generateRandomStrings(int numStrings, int avgLength) {
        String result[] = new String[numStrings];
        for (int i = 0; i < result.length; i++) {
            result[i] = createRandomString(avgLength);
        }
        return result;
    }

    private static String createRandomString(int avgLength) {
        int plusMinus = 30;
        int length = avgLength - plusMinus + RANDOM.nextInt(2 * plusMinus);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + RANDOM.nextInt('z' - 'a')));
        }
        return sb.toString();
    }

    @Parameterized.Parameters(name = "{0} threads")
    public static Object[] data() {
        return new Object[]{1, 10, 20, 30 , 40, 50, 75, 100, 150, 200, 300, 500, 1000};
    }

    private final int numThreads;

    private final SortedSet<String> result = new TreeSet<String>();

    private static final Map<Strategy, Long> totalElapsed = new HashMap<Strategy, Long>();

    public ConcurrentSetPerformanceTest(int numThreads) {
        this.numThreads = numThreads;
    }

    @BeforeClass
    public static void warmUpJitCompiler() throws Exception {
        for (Strategy strategy : STRATEGIES) {
            for (int i = 0; i < 200000; i++) {
                strategy.add(RANDOM_STRINGS[i % RANDOM_STRINGS.length]);
            }
            strategy.clear();
        }
        collectGarbage();
    }

    @AfterClass
    public static void showTheWinner() {
        SortedMap<Long, Strategy> ranking = new TreeMap<Long, Strategy>();
        for (Map.Entry<Strategy, Long> entry : totalElapsed.entrySet()) {
            ranking.put(entry.getValue(), entry.getKey());
        }

        System.out.printf("%4s %8s %s%n", "Rank", "Elapsed", "Strategy");
        System.out.println("-------------------------------------");
        int rank = 1;
        for (Map.Entry<Long, Strategy> entry : ranking.entrySet()) {
            System.out.printf("#%d: %5d ms  %s%n", rank, entry.getKey(), entry.getValue().getClass().getSimpleName());
            rank += 1;
        }

    }

    private static void collectGarbage() throws InterruptedException {
        System.gc();
        // Allow the GC thread to do it's job...
        Thread.sleep(100);
    }

    @After
    public void afterTest() throws Exception {
        for (String s : result) {
            System.out.println(s);
        }
        System.out.println();
    }

    @Test
    // @Ignore("Performance test for various concurrent set implementations")
    public void testConcurrentAddToSet() throws Exception {
        for (Strategy strategy : STRATEGIES) {
            if (strategy.isThreadSafe() || numThreads == 1) {
                long elapsed = doConcurrentAdds(strategy);
                result.add(String.format("%4d threads: %5d ms: %s", numThreads, elapsed, strategy.getClass().getSimpleName()));

                if (numThreads > 1) {
                    Long total = totalElapsed.get(strategy);
                    if (total == null) {
                        total = 0L;
                    }
                    totalElapsed.put(strategy, total + elapsed);
                }

                strategy.clear();
                collectGarbage();
            }
        }
    }

    private long doConcurrentAdds(final Strategy strategy) throws InterruptedException {
        final int count = 1000000 / numThreads;
        final CountDownLatch startingLine = new CountDownLatch(1);
        final CountDownLatch allThreadsFinished = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Maximize lock contention...
                        startingLine.await();

                        for (int i = 0; i < count; i++) {
                            strategy.add(RANDOM_STRINGS[i % RANDOM_STRINGS.length]);
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        allThreadsFinished.countDown();
                    }
                }
            });
            t.start();
        }

        long startedAt = System.currentTimeMillis();
        startingLine.countDown();
        allThreadsFinished.await();
        return System.currentTimeMillis() - startedAt;
    }
}
