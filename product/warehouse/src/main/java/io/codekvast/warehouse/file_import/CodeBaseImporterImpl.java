package io.codekvast.warehouse.file_import;

import io.codekvast.agent.lib.model.v1.CodeBasePublication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class CodeBaseImporterImpl implements CodeBaseImporter {

    @Override
    public void importPublication(CodeBasePublication publication) {
        log.debug("Importing {}", publication);
        // TODO: implement
    }
}
