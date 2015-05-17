package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.GetMethodUsageRequest;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageReport;

/**
 * @author olle.hallin@crisp.se
 */
public interface ReportService {

    /**
     * Prepares a preview of a MethodUsageReport. The report is cached for 10 minutes, and can be retrieved by {@link
     * #getMethodUsageReport(String, int)}.
     *
     * @param username The logged in user.
     * @param request  The request filter.
     * @return A MethodUsageReport object. Does never return null. The methods fields contains at most request.maxPreviewRows elements.
     */
    MethodUsageReport getMethodUsagePreview(String username, GetMethodUsageRequest request) throws CodekvastException;

    /**
     * Retrieve a full report prepared by {@link #getMethodUsagePreview(String, GetMethodUsageRequest)}.
     *
     * @param username The name of the user who invoked {@link #getMethodUsagePreview(String, GetMethodUsageRequest)}
     * @param reportId The reportId in the {@link MethodUsageReport} produced by {@link #getMethodUsagePreview(String,
     *                 GetMethodUsageRequest)}
     * @return A full report object
     * @throws IllegalArgumentException if invalid username or reportId or if the report has expired.
     */
    MethodUsageReport getMethodUsageReport(String username, int reportId);

    /**
     * Scheduled method that removes expired reports.
     */
    void reportScavenger();
}
