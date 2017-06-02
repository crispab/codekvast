/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
package io.codekvast.warehouse.retention;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Periodically performs data retention, i.e., removes data according to customers' price plans.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class RetentionTask {

    private final RetentionService retentionService;

    @Inject
    public RetentionTask(RetentionService retentionService) {
        this.retentionService = retentionService;
    }

    /**
     * Scheduled task that invokes the data retention service.
     */
    @Scheduled(
        initialDelayString = "${codekvast.dataRetentionInitialDelaySeconds}000",
        fixedDelayString = "${codekvast.dataRetentionIntervalSeconds}000")
    public void performDataRetention() {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("Codekvast Data Retention");
        try {
            log.info("Performing data retention");
            retentionService.performDataRetention();
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

}
