package se.crisp.duck.server.duck_server.db.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.duck.server.agent.model.v1.SignatureData;
import se.crisp.duck.server.duck_server.db.AgentService;

import javax.inject.Inject;

/**
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public AgentServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void storeSignatureData(SignatureData data) {
        log.debug("Storing {}", data);
    }
}
