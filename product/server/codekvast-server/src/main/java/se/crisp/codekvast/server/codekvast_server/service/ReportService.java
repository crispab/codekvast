package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.GetMethodUsageRequest;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.GetMethodUsageResponse;

/**
 * @author olle.hallin@crisp.se
 */
public interface ReportService {
    /**
     * Retrieves method usage.
     *
     * @param username The logged in user.
     * @param request  The request filter.
     * @return A GetMethodUsageResponse object. Does never return null.
     */
    GetMethodUsageResponse getMethodUsage(String username, GetMethodUsageRequest request) throws CodekvastException;

}
