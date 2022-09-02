/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.intake.agent.controller

import io.codekvast.common.logging.LoggerDelegate
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.util.NestedServletException
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

@Component
class LogAndSuppressRemoteClosedConnectionException : GenericFilterBean() {

    private val log by LoggerDelegate()

    /**
     * Catch a NestedServletException with the message
     *
     *
     * "Request processing failed; nested exception is
     * org.springframework.web.multipart.MultipartException: Failed to parse multipart servlet
     * request; nested exception is java.lang.RuntimeException: java.io.IOException: UT000128: Remote
     * peer closed connection before all data could be read"
     *
     *
     * Log it as a warning and continue.
     */
    override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        try {
            chain.doFilter(req, res)
        } catch (e: NestedServletException) {
            if (e.message!!.contains("UT000128: Remote peer closed connection")) {
                log.warn("{}. Request={}", e.message, req)
            } else {
                throw e
            }
        }
    }
}