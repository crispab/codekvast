/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
package io.codekvast.dashboard.weeding

import io.codekvast.common.lock.Lock
import io.codekvast.common.lock.LockTemplate
import io.codekvast.common.thread.NamedThreadTemplate
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
@Inject constructor(private val lockTemplate: LockTemplate,
                    private val weedingService: WeedingService) {

    /**
     * A scheduled task that invokes the data weeding service.
     */
    @Scheduled(initialDelayString = "\${codekvast.dashboard.dataWeedingInitialDelaySeconds:600}000",
        fixedDelayString = "\${codekvast.dashboard.dataWeedingIntervalSeconds:3600}000")
    fun performDataWeeding() {
        NamedThreadTemplate().doInNamedThread("Weeder") {
            lockTemplate.doWithLock(Lock.forTask("weeder")) {
                weedingService.findWeedingCandidates()
                weedingService.performDataWeeding()
            }
        }
    }

}
