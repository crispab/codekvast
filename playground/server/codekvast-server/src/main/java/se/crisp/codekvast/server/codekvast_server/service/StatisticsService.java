package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.codekvast_server.model.AppId;

import java.util.Collection;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
public interface StatisticsService {

    void recalculateApplicationStatistics(AppId appId);

    void recalculateApplicationStatistics(long organisationId, Collection<String> applicationNames);
}
