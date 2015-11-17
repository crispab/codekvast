package se.crisp.codekvast.warehouse.file_import;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileMetaInfo;

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
    public boolean isFileImported(ExportFileMetaInfo metaInfo) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_meta_info WHERE uuid = ? ", Integer.class, metaInfo.getUuid()) > 0;
    }

    @Override
    @Transactional
    public void recordFileAsImported(ExportFileMetaInfo metaInfo) {
        jdbcTemplate.update("INSERT INTO file_meta_info(uuid, fileName, fileLengthBytes, importedFromDaemonHostname) VALUES (?, ?, ?, ?)",
                            metaInfo.getUuid(), metaInfo.getFileName(), metaInfo.getFileLengthBytes(), metaInfo.getDaemonHostname());
        log.info("Imported {}", metaInfo);
    }
}
