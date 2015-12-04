package se.crisp.codekvast.agent.collector;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;
import org.junit.runners.Parameterized;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.concurrent.*;
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
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NotThreadSafe {}

    @RunListener.ThreadSafe
    static class BlockingQueueStrategy implements Strategy {
        private final Set<String> set = new HashSet<String>();
        private final BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
        private final Thread consumer = new Thread(new Runnable() {

            @Override
            public void run() {
                while(true) {
                    try {
                        set.add(queue.take());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });

        {
            consumer.start();
        }

        @Override
        public void add(String s) {
            queue.add(s);
        }

        @Override
        public synchronized void clear() {
            set.clear();
            queue.clear();
        }
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
    }

    @NotThreadSafe
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
    }

    private static final Strategy STRATEGIES[] = {
            new ConcurrentSkipListStrategy(),
            new SetFromConcurrentSkipListMapStrategy(),
            new SetFromConcurrentHashMapStrategy(),
            new ConcurrentHashMapStrategy(),
            new ManuallySynchronizedHashSetStrategy(),
            new ReadWriteLockHashSetStrategy(),
            new WrappedSynchronizedHashSetStrategy(),
            new UnsynchronizedHashSetStrategy(),
            new BlockingQueueStrategy()
    };

    private static final Random RANDOM = new Random();

    private static final String RANDOM_STRINGS[] = generateRandomStrings(100, 100);

    private static String[] generateRandomStrings(int numStrings, int avgLength) {
        String strings[] = new String[numStrings];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = createRandomString(avgLength);
        }
        return strings;
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

    @Value
    @Builder
    @EqualsAndHashCode(of = "strategy")
    static class Result implements Comparable<Result> {
        private final Long elapsedMillis;
        private final String strategy;

        String getMessage() {
            return String.format("%4d ms: %s", elapsedMillis, strategy);
        }

        @Override
        public int compareTo(Result that) {
            return this.elapsedMillis.compareTo(that.elapsedMillis);
        }
    }

    private final List<Result> result = new ArrayList<Result>();

    private static final Map<String, Integer> sumRanks = new HashMap<String, Integer>();

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
        SortedMap<Integer, String> ranking = new TreeMap<Integer, String>();
        for (Map.Entry<String, Integer> entry : sumRanks.entrySet()) {

            ranking.put(entry.getValue(), entry.getKey());
        }

        System.out.println("  Avg.Rank Strategy (>50 threads");
        System.out.println("--------------------------------");
        int rank = 1;
        for (Map.Entry<Integer, String> entry : ranking.entrySet()) {
            System.out.printf("#%d: %5.1f  %s%n", rank, (float)entry.getKey()/(sumRanks.size()-1f), entry.getValue());
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
        System.out.printf("%d threads:%n", numThreads);
        System.out.println("-------------");

        Collections.sort(result);
        int rank = 1;
        for (Result r : result) {
            System.out.printf("#%2d: %s%n", rank, r.getMessage());
            if (numThreads > 50) {
                Integer sumRank = sumRanks.get(r.getStrategy());
                if (sumRank == null) {
                    sumRank = 0;
                }
                sumRanks.put(r.getStrategy(), sumRank + rank);
            }
            rank += 1;
        }
        System.out.println();
    }

    @Test
    // @Ignore("Performance test for various concurrent set implementations")
    public void testConcurrentAddToSet() throws Exception {
        for (Strategy strategy : STRATEGIES) {
            if (strategy.getClass().getAnnotation(NotThreadSafe.class) == null || numThreads == 1) {
                long elapsed = doConcurrentAdds(strategy);
                result.add(Result.builder().elapsedMillis(elapsed).strategy(strategy.getClass().getSimpleName()).build());

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
