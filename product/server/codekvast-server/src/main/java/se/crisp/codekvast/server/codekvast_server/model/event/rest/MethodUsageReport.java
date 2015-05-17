package se.crisp.codekvast.server.codekvast_server.model.event.rest;

import lombok.*;
import org.springframework.stereotype.Component;

import java.beans.PropertyEditorSupport;
import java.util.Collection;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class MethodUsageReport {

    /**
     * In which formats can we fetch MethodUsageReports?
     */
    @RequiredArgsConstructor
    public enum Format {
        CSV("application/csv"), JSON("application/json");

        @Getter
        private final String contentType;

        public String getFilenameExtension() {
            return name().toLowerCase();
        }
    }

    @NonNull
    private final GetMethodUsageRequest request;

    @NonNull
    private final String username;

    private final int reportId;

    private long reportExpiresAtMillis;

    private final Map<MethodUsageScope, Integer> numMethodsByScope;
    private final Collection<MethodUsageEntry> methods;

    private final Collection<Format> availableFormats;

    /**
     * Spring converter to support lower case format parameters
     */
    @Component
    public static class MethodUsageReportFormatEnumConverter extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            setValue(Format.valueOf(text.toUpperCase()));
        }
    }
}
