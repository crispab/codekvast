package io.codekvast.backoffice.rules.impl;

import com.google.gson.Gson;
import io.codekvast.backoffice.rules.FactDAO;
import io.codekvast.common.messaging.model.CodekvastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLDataException;
import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class FactDAOImpl implements FactDAO {

    private final JdbcTemplate jdbcTemplate;
    private final Gson gson = new Gson();

    @Override
    public void addFact(CodekvastEvent event) {
        jdbcTemplate.update("INSERT INTO facts(customerId, type, data) VALUES(?, ?, ?)",
                            event.getCustomerId(), event.getClass().getName(), gson.toJson(event));
    }

    @Override
    public List<Object> getFacts(Long customerId) {
        List<Object> facts = jdbcTemplate.query("SELECT type, data FROM facts WHERE customerId = ?", (rs, rowNum) -> {
            String type = rs.getString("type");
            try {
                return gson.fromJson(rs.getString("data"), Class.forName(type));
            } catch (ClassNotFoundException e) {
                throw new SQLDataException("Cannot load fact of type '" + type + "'", e);
            }
        }, customerId);

        logger.trace("Retrieved the facts {} for customer {}", facts, customerId);
        logger.debug("Retrieved {} facts for customer {}", facts.size(), customerId);
        return facts;
    }

}
