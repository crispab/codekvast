/*
 * Copyright (c) 2015-2017 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
     *     <li>In IDEA: Right-click a method name -&gt; Copy Reference (Ctrl-Alt-Shift-C)</li>
     *     <li>In Codekvast Warehouse web UI: paste into the search field (Ctrl-V)</li>
     * </ol>
     *
     * @param request The request parameters
     * @return A list of matching methods. Does never return null.
     */
    @NotNull
    List<MethodDescriptor1> getMethods(@Valid GetMethodsRequest1 request);

}
