package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olle Hallin
 */
public abstract class AbstractServiceTest extends AbstractTransactionalJUnit4SpringContextTests {
    protected final long startedAtMillis = System.currentTimeMillis() - 3600_000L;
    protected final long now = System.currentTimeMillis();

    protected final List<Object> events = new ArrayList<>();

    @Inject
    private EventBus eventBus;

    @Before
    public void before() throws Exception {
        eventBus.register(this);
    }

    /*
         * The eventBus is asynchronous, events are delivered on another thread than the service call.
         */
    protected void assertEventsWithinMillis(int expectedEvents, long maxWaitMillis) {
        long stopWaitAt = System.currentTimeMillis() + maxWaitMillis;

        while (true) {
            synchronized (events) {
                int currentLength = events.size();
                if (currentLength >= expectedEvents) {
                    return;
                }
            }
            try {
                Thread.sleep(5L);
            } catch (InterruptedException ignore) {
            }

            if (System.currentTimeMillis() > stopWaitAt) {
                throw new AssertionFailedError("Expected " + expectedEvents + " within " + maxWaitMillis + " ms.");
            }
        }

    }

    protected Integer countRows(String table, Object... args) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class, args);
    }
}
