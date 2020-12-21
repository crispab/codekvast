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
package io.codekvast.intake.service.impl

import io.codekvast.intake.model.PublicationType
import io.codekvast.intake.service.IntakeService
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1
import io.codekvast.javaagent.model.v2.GetConfigRequest2
import io.codekvast.javaagent.model.v2.GetConfigResponse2
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.*
import java.util.function.Function
import java.util.regex.Pattern
import java.util.stream.Collectors


/**
 * @author olle.hallin@crisp.se
 */
@Service
class IntakeServiceImpl : IntakeService {
    val UNKNOWN_ENVIRONMENT = "<UNKNOWN>"
    val CORRELATION_ID_PATTERN = buildCorrelationIdPattern()

    private fun buildCorrelationIdPattern(): Pattern {
        val publicationTypes: String =
            Arrays.stream(PublicationType.values())
                .map(PublicationType::toString)
                .collect(Collectors.joining("|", "(", ")"))
        return Pattern.compile("""$publicationTypes-([0-9]+)-([a-fA-F0-9_-]+)\.ser$""")
    }

    override fun getConfig1(request: GetConfigRequest1): GetConfigResponse1 {
        TODO("Not yet implemented")
    }

    override fun getConfig2(request: GetConfigRequest2): GetConfigResponse2 {
        TODO("Not yet implemented")
    }

    override fun savePublication(
        publicationType: PublicationType,
        licenseKey: String,
        codebaseFingerprint: String,
        publicationSize: Int,
        inputStream: InputStream
    ) {
        TODO("Not yet implemented")
    }
}