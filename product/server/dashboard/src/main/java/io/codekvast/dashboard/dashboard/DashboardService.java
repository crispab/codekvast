/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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
package io.codekvast.dashboard.dashboard;

import io.codekvast.dashboard.dashboard.model.methods.GetMethodsFormData;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsRequest;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsResponse2;
import io.codekvast.dashboard.dashboard.model.methods.MethodDescriptor1;
import io.codekvast.dashboard.dashboard.model.status.GetStatusResponse;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * The service used by the {@link DashboardApiController}.
 *
 * @author olle.hallin@crisp.se
 */
public interface DashboardService {

    /**
     * Retrieve information about a set of methods.
     *
     * Use case: <ol> <li>In IDEA: Right-click a method name -&gt; Copy Reference (Ctrl-Alt-Shift-C)</li> <li>In Codekvast Dashboard web UI:
     * paste into the search field (Ctrl-V)</li> </ol>
     *
     * @param request The request parameters.
     * @return A response object. Does never return null.
     */
    GetMethodsResponse2 getMethods2(@Valid GetMethodsRequest request);

    /**
     * Retrieve information about a particular method.
     *
     * @param methodId The primary key of the method.
     * @return an optional MethodDescriptor1. Does never return null.
     */
    Optional<MethodDescriptor1> getMethodById(@NotNull Long methodId);

    /**
     * Retrieve status for the authenticated customer.
     *
     * @return A status object. Does never return null.
     */
    GetStatusResponse getStatus();

    /**
     * Get data to use in the search methods form.
     *
     * @return A GetMethodsFormData object. Does never return null.
     */
    GetMethodsFormData getMethodsFormData();
}
