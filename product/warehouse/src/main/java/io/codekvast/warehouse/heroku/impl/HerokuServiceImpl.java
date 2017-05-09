package io.codekvast.warehouse.heroku.impl;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.heroku.HerokuException;
import io.codekvast.warehouse.heroku.HerokuProvisionRequest;
import io.codekvast.warehouse.heroku.HerokuProvisionResponse;
import io.codekvast.warehouse.heroku.HerokuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class HerokuServiceImpl implements HerokuService {

    private final CodekvastSettings settings;
    private final JdbcTemplate jdbcTemplate;

    @Inject
    public HerokuServiceImpl(CodekvastSettings settings, JdbcTemplate jdbcTemplate) {
        this.settings = settings;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public HerokuProvisionResponse provision(HerokuProvisionRequest request) throws HerokuException {
        log.debug("Handling {}", request);

        String licenseKey = UUID.randomUUID().toString().replaceAll("[-_]", "").toUpperCase();

        jdbcTemplate.update("INSERT INTO customers(externalId, name, licenseKey, plan) VALUES(?, ?, ?, ?)",
                            request.getUuid(), request.getHeroku_id(), licenseKey, request.getPlan());

        Map<String, String> config = new HashMap<>();
        config.put("CODEKVAST_APP_NAME", request.getHeroku_id());
        config.put("CODEKVAST_LICENSE_KEY", licenseKey);
        config.put("CODEKVAST_URL", settings.getHerokuCodekvastUrl());

        HerokuProvisionResponse response = HerokuProvisionResponse.builder()
                                                               .id(request.getUuid())
                                                               .config(config)
                                                               .message("You also need to add codekvast.conf to your application!")
                                                               .build();
        log.debug("Returning {}", response);
        return response;
    }

    @Override
    @Transactional
    public void deprovision(String externalId) throws HerokuException {
        Long customerId = jdbcTemplate.queryForObject("SELECT id FROM customers WHERE externalId = ?", Long.class, externalId);
        if (customerId == null) {
            log.warn("Invalid customer.externalId: {}", externalId);
            return;
        }

        int count = jdbcTemplate.update("DELETE FROM invocations WHERE customerId = ?", customerId);
        log.debug("Deleted {} invocation rows", count);

        count = jdbcTemplate.update("DELETE FROM methods WHERE customerId = ?", customerId);
        log.debug("Deleted {} method rows", count);

        count = jdbcTemplate.update("DELETE FROM jvms WHERE customerId = ?", customerId);
        log.debug("Deleted {} method rows", count);

        count = jdbcTemplate.update("DELETE FROM applications WHERE customerId = ?", customerId);
        log.debug("Deleted {} application rows", count);

        count = jdbcTemplate.update("DELETE FROM customers WHERE id = ?", customerId);
        log.debug("Deleted {} customer rows", count);
    }
}
