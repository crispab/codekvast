package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author olle.hallin@crisp.se
 */
public abstract class AbstractServiceIntegTest extends AbstractTransactionalJUnit4SpringContextTests {
    protected final List<Object> events = new CopyOnWriteArrayList<>();

    @Inject
    private EventBus eventBus;

    @Before
    public void before() throws Exception {
        eventBus.register(this);
        events.clear();
    }

    protected Integer countRows(String table, Object... args) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class, args);
    }
}
