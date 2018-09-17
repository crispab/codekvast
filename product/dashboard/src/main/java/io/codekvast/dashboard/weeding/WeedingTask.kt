/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
package io.codekvast.dashboard.weeding

import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.inject.Inject

/**
 * Periodically performs data weeding, i.e., remove redundant data that does not affect what a customer sees.
 *
 * @author olle.hallin@crisp.se
 */
@Component
class WeedingTask
    @Inject constructor(private val weedingService: WeedingService) {

    /**
     * Scheduled task that invokes the data weeding service.
     */
    @Scheduled(initialDelayString = "\${codekvast.dataWeedingInitialDelaySeconds}000", fixedDelayString = "\${codekvast.dataWeedingIntervalSeconds}000")
    fun performDataWeeding() {
        val oldThreadName = Thread.currentThread().name
        Thread.currentThread().name = "Codekvast Data Weeder"
        try {
            weedingService.performDataWeeding()
        } catch (e: CannotAcquireLockException) {
            logger.warn("Could not perform data weeding: $e")
        } catch (e: Exception) {
            logger.error("Could not perform data weeding", e)
        } finally {
            Thread.currentThread().name = oldThreadName
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WeedingTask::class.java.name)!!
    }

}
