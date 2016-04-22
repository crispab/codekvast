package se.crisp.codekvast.warehouse.api;

import se.crisp.codekvast.warehouse.api.model.MethodDescriptor;

import java.util.List;

/**
 * A service for querying the warehouse for collected information.
 *
 * @author olle.hallin@crisp.se
 */
public interface QueryService {

    /**
     * Retrieve information about a certain method.
     *
     * Use case: 1. In IDEA: Right-click a method name -> Copy Reference (Ctrl-Alt-Shift-C) 2. In Codekvast Warehouse web UI: paste into the
     * search field (Ctrl-V)
     *
     * @param signature The signature to search for. May be null, which means get all methods.
     * @param maxResults
     * @return A list of matching methods. Does never return null.
     */
    List<MethodDescriptor> findMethodsBySignature(String signature, int maxResults);
}
