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
package io.codekvast.intake.file_import.impl

import io.codekvast.common.customer.CustomerService
import io.codekvast.javaagent.model.v2.CommonPublicationData2
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Helper for importing common stuff.
 *
 * @author olle.hallin@crisp.se
 */
@Component
class CommonImporter(
        private val importDAO: ImportDAO,
        private val customerService: CustomerService
) {

    fun importCommonData(data: CommonPublicationData2): ImportContext {
        val appId = importDAO.importApplication(data)
        val environmentId = importDAO.importEnvironment(data)
        importDAO.upsertApplicationDescriptor(data, appId, environmentId)
        val jvmId = importDAO.importJvm(data, appId, environmentId)

        return ImportContext(
                customerId = data.customerId,
                appId = appId,
                environmentId = environmentId,
                jvmId = jvmId,
                publishedAtMillis = data.publishedAtMillis,
                trialPeriodEndsAt = customerService
                        .getCustomerDataByCustomerId(data.customerId)
                        .trialPeriodEndsAt
        )
    }

    data class ImportContext(
            val customerId: Long,
            val appId: Long,
            val environmentId: Long,
            val jvmId: Long,
            val publishedAtMillis: Long,
            val trialPeriodEndsAt: Instant?
    )

}