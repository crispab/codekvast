package se.crisp.codekvast.agent.collector;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This is an elaboratory test for measuring performance for different implementations of concurrent sets.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
@RunWith(Parameterized.class)
public class ConcurrentSetPerformanceTest {

    private static final int NUM_DIFFERENT_STRINGS = 1000;
    private static final int AVG_STRING_LENGTH = 80;

    interface Strategy {
        void initialize(int numThreads);
        void add(String s);

        boolean contains(String s);
        void clear();
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NotThreadSafe {}

    static class BlockingQueueStrategy implements Strategy {
        private final Set<String> set = new HashSet<String>();
        private final BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
        private Thread consumer;

        @Override
        public void initialize(int numThreads) {
            consumer = new Thread(new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        try {
                            set.add(queue.take());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }
            });
            consumer.start();
        }

        @Override
        public void add(String s) {
            if (!set.contains(s)) {
                queue.add(s);
            }
        }

        @Override
        public boolean contains(String s) {
            while (!queue.isEmpty()) {
                ;
            }
            return set.contains(s);
        }

        @Override
        public synchronized void clear() {
            set.clear();
            queue.clear();
        }
    }

    static class AtomicReferenceWrappedRegularHashSet implements Strategy {
        private final AtomicReference<Set<String>> ref = new AtomicReference<Set<String>>(new HashSet<String>());
        private int numThreads;

        @Override
        public void initialize(int numThreads) {
            this.numThreads = numThreads;
        }

        @Override
        public void add(String s) {
            while (true) {
                Set<String> current = ref.get();
                if (current.contains(s)) {
                    return;
                }

                if (numThreads <= 1) {
                    ref.get().add(s);
                    return;
                }

                Set<String> modified = new HashSet<String>(current);
                modified.add(s);
                if (ref.compareAndSet(current, modified)) {
                    return;
                }
            }
        }

        @Override
        public boolean contains(String s) {
            return ref.get().contains(s);
        }

        @Override
        public void clear() {
            ref.get().clear();
        }
    }

    static class ConcurrentSkipListStrategy implements Strategy {
        private final Set<String> set = new ConcurrentSkipListSet<String>();

        @Override
        public void initialize(int numThreads) {

        }

        @Override
        public void add(String s) {
            set.add(s);
        }

        @Override
        public boolean contains(String s) {
            return set.contains(s);
        }

        @Override
        public void clear() {
            set.clear();
        }
    }

    static class SetFromConcurrentHashMapStrategy implements Strategy {
        private Set<String> set;

        @Override
        public void initialize(int numThreads) {
            set = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(NUM_DIFFERENT_STRINGS, 0.9f, numThreads));
        }

        @Override
        public void add(String s) {
            set.add(s);
        }

        @Override
        public boolean contains(String s) {
            return set.contains(s);
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
        public void initialize(int numThreads) {

        }

        @Override
        public void add(String s) {
            set.add(s);
        }

        @Override
        public boolean contains(String s) {
            return set.contains(s);
        }

        @Override
        public void clear() {
            set.clear();
        }
    }

    private static final Strategy STRATEGIES[] = {
            new AtomicReferenceWrappedRegularHashSet(),
            new ConcurrentSkipListStrategy(),
            new SetFromConcurrentHashMapStrategy(),
            new UnsynchronizedHashSetStrategy(),
            new BlockingQueueStrategy()
    };

    private static final Random RANDOM = new Random();

    private static final String RANDOM_STRINGS[] = generateRandomStrings(NUM_DIFFERENT_STRINGS, AVG_STRING_LENGTH);

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

    @Parameterized.Parameters(name = "{0} threads")
    public static Object[] data() {
        return new Object[]{1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 750, 1000};
    }

    private final int numThreads;

    private final List<Result> result = new ArrayList<Result>();

    private static final Map<String, Integer> sumRanks = new HashMap<String, Integer>();
    private static final Map<String, Long> sumElapsed = new HashMap<String, Long>();
    private static final int cutoffThreads = 20;
    private static int numTestsWithMoreThanCutoffThreads;

    public ConcurrentSetPerformanceTest(int numThreads) {
        this.numThreads = numThreads;
        if (numThreads > cutoffThreads) {
            numTestsWithMoreThanCutoffThreads += 1;
        }
    }

    @BeforeClass
    public static void warmUpJitCompiler() throws Exception {
        for (Strategy strategy : STRATEGIES) {
            strategy.initialize(1);
            for (int i = 0; i < 200000; i++) {
                strategy.add(RANDOM_STRINGS[i % RANDOM_STRINGS.length]);
            }
            strategy.clear();
        }
        collectGarbage();
    }

    @AfterClass
    public static void showTheWinner() {
        if (sumRanks.isEmpty()) {
            return;
        }

        SortedMap<Integer, String> ranking = new TreeMap<Integer, String>();
        for (Map.Entry<String, Integer> entry : sumRanks.entrySet()) {

            ranking.put(entry.getValue(), entry.getKey());
        }

        System.out.printf("  Avg.Rank Sum Elapsed (ms) Strategy (>%d threads)%n", cutoffThreads);
        System.out.println("--------------------------------------------------");
        int rank = 1;
        for (Map.Entry<Integer, String> entry : ranking.entrySet()) {
            System.out
                    .printf("#%d: %5.1f %17d %s%n", rank, (float) entry.getKey() / numTestsWithMoreThanCutoffThreads,
                            sumElapsed.get(entry.getValue()),
                            entry.getValue());
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
            if (numThreads > cutoffThreads) {
                Integer sumRank = sumRanks.get(r.getStrategy());
                if (sumRank == null) {
                    sumRank = 0;
                }
                sumRanks.put(r.getStrategy(), sumRank + rank);

                Long elapsed = sumElapsed.get(r.getStrategy());
                if (elapsed == null) {
                    elapsed = 0L;
                }
                sumElapsed.put(r.getStrategy(), elapsed + r.getElapsedMillis());
            }
            rank += 1;
        }
        System.out.println();
    }

    @Test
    @Ignore
    public void testConcurrentAddToSet() throws Exception {
        for (Strategy strategy : STRATEGIES) {
            strategy.initialize(numThreads);

            if (strategy.getClass().getAnnotation(NotThreadSafe.class) == null || numThreads == 1) {
                long elapsed = doConcurrentAdds(strategy);
                result.add(Result.builder().elapsedMillis(elapsed).strategy(strategy.getClass().getSimpleName()).build());

                for (String s : RANDOM_STRINGS) {
                    assertThat(strategy.contains(s), is(true));
                }

                strategy.clear();
                collectGarbage();
            }
        }
    }

    private long doConcurrentAdds(final Strategy strategy) throws InterruptedException {
        final CountDownLatch startingLine = new CountDownLatch(1);
        final CountDownLatch allThreadsFinished = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Maximize lock contention...
                        startingLine.await();

                        for (int i = 0; i < RANDOM_STRINGS.length; i++) {
                            strategy.add(RANDOM_STRINGS[i]);
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
