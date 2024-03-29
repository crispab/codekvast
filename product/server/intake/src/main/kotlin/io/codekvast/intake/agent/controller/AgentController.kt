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
import io.codekvast.common.logging.LoggingUtils.humanReadableByteCount
import io.codekvast.intake.agent.service.AgentService
import io.codekvast.intake.model.PublicationType
import io.codekvast.intake.model.PublicationType.CODEBASE
import io.codekvast.intake.model.PublicationType.INVOCATIONS
import io.codekvast.javaagent.model.Endpoints.Agent.*
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1
import io.codekvast.javaagent.model.v2.GetConfigRequest2
import io.codekvast.javaagent.model.v2.GetConfigResponse2
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.inject.Inject
import javax.validation.Valid

/**
 * The codekvast-javaagent REST controller.
 *
 * @author olle.hallin@crisp.se
 */
@RestController
@RequestMapping(consumes = [MULTIPART_FORM_DATA_VALUE], produces = [TEXT_PLAIN_VALUE])
class AgentController @Inject constructor(private val agentService: AgentService) {

    private val logger by LoggerDelegate()

    @ExceptionHandler
    fun onLicenseViolationException(e: io.codekvast.intake.model.LicenseViolationException): ResponseEntity<String> {
        logger.warn("Rejected request: {}", e.message)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
    }

    @Suppress("DEPRECATION")
    @PostMapping(
            value = [V1_POLL_CONFIG],
            consumes = [APPLICATION_JSON_UTF8_VALUE, APPLICATION_JSON_VALUE],
            produces = [APPLICATION_JSON_VALUE]
    )
    fun getConfig1(@Valid @RequestBody request: GetConfigRequest1): GetConfigResponse1 {
        logger.debug("Received {}", request)

        val response = agentService.getConfig1(request)

        logger.debug("Responds with {}", response)
        return response
    }

    @Suppress("DEPRECATION")
    @PostMapping(
            value = [V2_POLL_CONFIG],
            consumes = [APPLICATION_JSON_UTF8_VALUE, APPLICATION_JSON_VALUE],
            produces = [APPLICATION_JSON_VALUE]
    )
    fun getConfig2(@Valid @RequestBody request: GetConfigRequest2): GetConfigResponse2 {
        logger.debug("Received {}", request)

        val response = agentService.getConfig2(request)

        logger.debug("Responds with {}", response)
        return response
    }

    @PostMapping(value = [V2_UPLOAD_CODEBASE, V3_UPLOAD_CODEBASE])
    fun uploadCodeBase(
            @RequestParam(PARAM_LICENSE_KEY) licenseKey: String,
            @RequestParam(PARAM_FINGERPRINT) fingerprint: String,
            @RequestParam(PARAM_PUBLICATION_SIZE) publicationSize: Int,
            @RequestParam(PARAM_PUBLICATION_FILE) file: MultipartFile
    ): String {

        saveUploadedPublication(CODEBASE, licenseKey, fingerprint, publicationSize, file)

        return "OK"
    }

    @PostMapping(value = [V2_UPLOAD_INVOCATION_DATA])
    fun uploadInvocationData2(
            @RequestParam(PARAM_LICENSE_KEY) licenseKey: String,
            @RequestParam(PARAM_FINGERPRINT) fingerprint: String,
            @RequestParam(PARAM_PUBLICATION_SIZE) publicationSize: Int,
            @RequestParam(PARAM_PUBLICATION_FILE) file: MultipartFile
    ): String {

        saveUploadedPublication(INVOCATIONS, licenseKey, fingerprint, publicationSize, file)

        return "OK"
    }

    private fun saveUploadedPublication(
            publicationType: PublicationType,
            licenseKey: String,
            fingerprint: String,
            publicationSize: Int,
            file: MultipartFile
    ) {

        logger.info(
                "Received {} ({} {}, {})",
                file.originalFilename,
                publicationSize,
                publicationType,
                humanReadableByteCount(file.size)
        )

        agentService.savePublication(
                publicationType,
                licenseKey,
                fingerprint,
                publicationSize,
                file.inputStream
        )
    }

}
