package se.crisp.codekvast.warehouse.file_import;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Service
@Slf4j
public class ImportServiceImpl implements ImportService {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public ImportServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public boolean isFileImported(String uuid) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_imports WHERE uuid = ? ", Integer.class, uuid) > 0;
    }

    @Override
    @Transactional
    public void recordFileAsImported(String uuid, long lengthBytes, String importedFromHostname) {
        jdbcTemplate.update("INSERT INTO file_imports(uuid, lengthBytes, importedFromDaemonHostname) VALUES (?, ?, ?)",
                            uuid, lengthBytes, importedFromHostname);
        log.info("File with uuid {} from {} imported", uuid, importedFromHostname);
    }
}
