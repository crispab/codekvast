package se.crisp.codekvast.warehouse.api;

import se.crisp.codekvast.warehouse.api.model.MethodDescriptor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

import static se.crisp.codekvast.warehouse.api.QueryMethodsBySignatureParameters.OrderBy;

/**
 * A service for querying the warehouse for collected information.
 *
 * @author olle.hallin@crisp.se
 */
public interface QueryService {

    interface Default {
        int MAX_RESULTS = 100;
        String MAX_RESULTS_STR = "" + MAX_RESULTS;
        OrderBy ORDER_BY = OrderBy.INVOKED_AT_ASC;
        boolean ONLY_TRULY_DEAD_METHODS = false;
        boolean NORMALIZE_SIGNATURE = true;
    }

    /**
     * Retrieve information about a certain method.
     *
     * Use case:
     * <ol>
     *     <li>In IDEA: Right-click a method name -> Copy Reference (Ctrl-Alt-Shift-C)</li>
     *     <li>In Codekvast Warehouse web UI: paste into the
     * search field (Ctrl-V)</li>
     * </ol>
     *
     * @param params The query parameters
     * @return A list of matching methods. Does never return null.
     */
    @NotNull
    List<MethodDescriptor> queryMethodsBySignature(@Valid QueryMethodsBySignatureParameters params);

}
