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
package io.codekvast.warehouse.webapp;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static io.codekvast.warehouse.webapp.WebappService.DEMO_CUSTOMER_ID;

/**
 * A servlet filter that makes sure there is a valid customerId for all endpoints within /webapp/**.
 *
 * It either picks the customerId from the HttpSession or uses the fallback value 1L.
 *
 * @author olle.hallin@crisp.se
 */
@WebFilter(urlPatterns = "/webapp/**")
@Slf4j
public class CustomerIdFilter implements Filter {
    private static final ThreadLocal<Long> customerIdHolder = new ThreadLocal<>();

    public static Long getCustomerId() {
        return customerIdHolder.get();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        customerIdHolder.set(getCustomerId((HttpServletRequest) request));
        try {
            chain.doFilter(request, response);
        } finally {
            customerIdHolder.remove();
        }
    }

    private Long getCustomerId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long customerId = session == null ? null : (Long) session.getAttribute("customerId");
        return customerId == null ? DEMO_CUSTOMER_ID : customerId;
    }

    @Override
    public void destroy() {
    }
}
