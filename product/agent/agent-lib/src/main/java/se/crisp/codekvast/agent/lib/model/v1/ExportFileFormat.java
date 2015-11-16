package se.crisp.codekvast.agent.lib.model.v1;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The supported export file formats.
 *
 * @author olle.hallin@crisp.se
 */
@RequiredArgsConstructor
@Getter
public enum ExportFileFormat {
    ZIP(".zip");

    private final String suffix;
}
