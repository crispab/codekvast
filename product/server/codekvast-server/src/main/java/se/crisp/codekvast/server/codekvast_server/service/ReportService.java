package se.crisp.codekvast.server.codekvast_server.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.GetMethodUsageRequest;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageReport;

/**
 * @author olle.hallin@crisp.se
 */
public interface ReportService {

    /**
     * Prepares a preview of a MethodUsageReport. The report is cached for 10 minutes, and can be retrieved by {@link
     * #getFormattedMethodUsageReport(String, int, se.crisp.codekvast.server.codekvast_server.service.ReportService.Format)}.
     *
     * @param username The logged in user.
     * @param request  The request filter.
     * @return A MethodUsageReport object. Does never return null. The methods fields contains at most request.maxPreviewRows elements.
     */
    MethodUsageReport getMethodUsagePreview(String username, GetMethodUsageRequest request) throws CodekvastException;

    /**
     * Retrieve a full formatted method usage report prepared by {@link #getMethodUsagePreview(String, GetMethodUsageRequest)}.
     *
     * @param username The name of the user who invoked {@link #getMethodUsagePreview(String, GetMethodUsageRequest)}
     * @param reportId The reportId in the {@link MethodUsageReport} produced by {@link #getMethodUsagePreview(String,
     *                 GetMethodUsageRequest)}
     * @param format The desired report format
     * @return A report in the desired format
     * @throws IllegalArgumentException if invalid username or reportId or if the report has expired.
     * @throws CodekvastException If the report cannot be produced.
     */
    String getFormattedMethodUsageReport(String username, int reportId, Format format) throws CodekvastException;

    /**
     * Scheduled method that removes expired reports. It must be exposed in this interface or else Spring will throw an exception.
     */
    void reportScavenger();

    /**
     * In which formats can we fetch MethodUsageReports?
     */
    @RequiredArgsConstructor
    enum Format {
        CSV("application/csv"), JSON("application/json"), XML("application/xml");

        @Getter
        private final String contentType;

        public String getFilenameExtension() {
            return name().toLowerCase();
        }
    }
}
