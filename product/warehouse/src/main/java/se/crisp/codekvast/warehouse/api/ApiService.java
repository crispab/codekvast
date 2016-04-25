package se.crisp.codekvast.warehouse.api;

import se.crisp.codekvast.warehouse.api.model.GetMethodsRequest1;
import se.crisp.codekvast.warehouse.api.model.MethodDescriptor1;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

import static se.crisp.codekvast.warehouse.api.model.GetMethodsRequest1.OrderBy;

/**
 * The service used by the {@link ApiController}.
 *
 * @author olle.hallin@crisp.se
 */
public interface ApiService {

    int DEFAULT_MAX_RESULTS = 100;
    String DEFAULT_MAX_RESULTS_STR = "" + DEFAULT_MAX_RESULTS;
    OrderBy DEFAULT_ORDER_BY = OrderBy.INVOKED_AT_ASC;
    boolean DEFAULT_ONLY_TRULY_DEAD_METHODS = false;
    boolean DEFAULT_NORMALIZE_SIGNATURE = true;

    /**
     * Retrieve information about a certain method or methods.
     *
     * Use case:
     * <ol>
     *     <li>In IDEA: Right-click a method name -> Copy Reference (Ctrl-Alt-Shift-C)</li>
     *     <li>In Codekvast Warehouse web UI: paste into the
     * search field (Ctrl-V)</li>
     * </ol>
     *
     * @param request The request parameters
     * @return A list of matching methods. Does never return null.
     */
    @NotNull
    List<MethodDescriptor1> getMethods(@Valid GetMethodsRequest1 request);

}
