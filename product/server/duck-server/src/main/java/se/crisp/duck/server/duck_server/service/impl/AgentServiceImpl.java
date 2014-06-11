package se.crisp.duck.server.duck_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.duck.server.agent.model.v1.SensorData;
import se.crisp.duck.server.agent.model.v1.SignatureData;
import se.crisp.duck.server.agent.model.v1.UsageData;
import se.crisp.duck.server.duck_server.service.AgentService;

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
    public void storeSensorData(SensorData data) {
        log.debug("Storing {}", data);
        // TODO: implement storing sensor data

    }

    @Override
    @Transactional
    public void storeSignatureData(SignatureData data) {
        log.debug("Storing {} signatures from {}", data.getSignatures().size(), data);
        // TODO: implement storing signature data
    }

    @Override
    @Transactional
    public void storeUsageData(UsageData data) {
        log.debug("Storing {} usages from {}", data.getUsage().size(), data);
        // TODO: implement storing usage data
    }
}
