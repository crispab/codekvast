package se.crisp.codekvast.collector;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;

/**
 * This is an elaboratory test for measuring performance for different implementations of concurrent sets.
 *
 * @author olle.hallin@crisp.se
 */
@RunWith(Parameterized.class)
public class ConcurrentSetPerformanceTest {

    interface Strategy {
        void add(String s);

        void clear();
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
        private Set<String> set = new HashSet<String>();

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

    static class WrappedSynchronizedHashSetStrategy implements Strategy {
        private Set<String> set = Collections.synchronizedSet(new HashSet<String>());

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
            new WrappedSynchronizedHashSetStrategy()
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
        return new Object[]{10, 50, 100, 200, 500};
    }

    @Parameterized.Parameter(0)
    public int numThreads;

    private SortedSet<String> result = new TreeSet<String>();

    @BeforeClass
    public static void warmUpJitCompiler() throws Exception {
        for (Strategy strategy : STRATEGIES) {
            for (int i = 0; i < 10000; i++) {
                strategy.add(RANDOM_STRINGS[i % RANDOM_STRINGS.length]);
            }
            strategy.clear();
        }
    }

    @Before
    public void beforeTest() throws Exception {
        for (Strategy strategy : STRATEGIES) {
            strategy.clear();
        }
    }

    @After
    public void afterTest() throws Exception {
        for (String s : result) {
            System.out.println(s);
        }
        System.out.println();
    }

    @Test
    @Ignore("Performance test for various concurrent set implementations")
    public void testConcurrentAddToSet() throws Exception {
        for (Strategy strategy : STRATEGIES) {
            long elapsed = doConcurrentAdds(strategy);
            result.add(String.format("%3d threads: %5d ms: %s", numThreads, elapsed, strategy.getClass().getSimpleName()));
        }
    }

    private long doConcurrentAdds(final Strategy strategy) throws InterruptedException {
        final int count = 10000000 / numThreads;
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
                    } catch (InterruptedException e) {
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
