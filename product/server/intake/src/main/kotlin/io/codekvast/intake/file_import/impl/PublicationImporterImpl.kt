/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
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

import io.codekvast.common.customer.LicenseViolationException
import io.codekvast.common.lock.Lock
import io.codekvast.common.lock.LockTemplate
import io.codekvast.common.lock.LockTimeoutException
import io.codekvast.common.logging.LoggerDelegate
import io.codekvast.common.messaging.CorrelationIdHolder
import io.codekvast.intake.agent.service.AgentService
import io.codekvast.intake.file_import.CodeBaseImporter
import io.codekvast.intake.file_import.InvocationDataImporter
import io.codekvast.intake.file_import.PublicationImporter
import io.codekvast.javaagent.model.v2.CodeBasePublication2
import io.codekvast.javaagent.model.v2.InvocationDataPublication2
import io.codekvast.javaagent.model.v3.CodeBaseEntry3
import io.codekvast.javaagent.model.v3.CodeBasePublication3
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import java.io.*
import java.util.stream.Collectors
import javax.validation.Validator

/**
 * Importer for serialized publications.
 *
 *
 * Deserialize the object and dispatch to the specialized importer.
 *
 * @author olle.hallin@crisp.se
 * @see CodeBasePublication2
 *
 * @see InvocationDataPublication2
 */
@Service
class PublicationImporterImpl(
        private val codeBaseImporter: CodeBaseImporter,
        private val invocationDataImporter: InvocationDataImporter,
        private val validator: Validator,
        private val agentService: AgentService,
        private val lockTemplate: LockTemplate
) : PublicationImporter {

    val logger by LoggerDelegate()

    override fun importPublicationFile(file: File): Boolean {
        return lockTemplate.doWithLock(
                Lock.forPublication(file),
                { doImportPublicationFile(file) },
                {
                    logger.info(
                            "Processing of {} was already in progress in another transaction",
                            file.name
                    )
                    false
                })
    }

    private fun doImportPublicationFile(file: File): Boolean {
        logger.info("Processing {}", file.name)
        var handled: Boolean
        CorrelationIdHolder.set(agentService.getCorrelationIdFromPublicationFile(file))
        try {
            ObjectInputStream(BufferedInputStream(FileInputStream(file))).use { ois ->
                val startedAt = System.currentTimeMillis()
                val obj: Any = ois.readObject()
                logger.debug(
                        "Deserialized a {} in {} ms",
                        obj.javaClass.simpleName,
                        System.currentTimeMillis() - startedAt
                )
                handled = !isValidObject(obj) || handlePublication(obj)
            }
        } catch (e: LockTimeoutException) {
            // A new attempt to process the file should be made in a new transaction.
            logger.warn(
                    "Could not import {}: {}. Will try again.",
                    file,
                    e.toString()
            )
            handled = false
        } catch (e: DataAccessException) {
            logger.warn(
                    "Could not import {}: {}. Will try again.",
                    file,
                    e.toString()
            )
            handled = false
        } catch (e: InvalidClassException) {
            // An incompatible publication file was lying in the queue.
            // The publication data is lost.
            // Prevent the file from being processed again.
            logger.error(
                    "Could not import {}: {}. Will not try again.",
                    file,
                    e.toString()
            )
            handled = true
        } catch (e: LicenseViolationException) {
            // Prevent the file from being processed again.
            // The agent will keep retrying uploading new publication files.
            logger.warn("Ignoring {}: {}", file, e.toString())
            handled = true
        } catch (e: Exception) {
            // A new attempt to process the file should be made.
            // Perhaps after deploying a new version of the service.
            logger.error("Could not import $file. Will try again.", e)
            handled = false
        } finally {
            CorrelationIdHolder.clear()
        }
        return handled
    }

    private fun handlePublication(obj: Any): Boolean {
        return when (obj) {
            is CodeBasePublication2 -> codeBaseImporter.importPublication(toCodeBasePublication3(obj))
            is CodeBasePublication3 -> codeBaseImporter.importPublication(obj)
            is InvocationDataPublication2 -> invocationDataImporter.importPublication(obj)
            else -> {
                logger.warn("Don't know how to handle {}", obj.javaClass.name)
                false
            }
        }
    }

    private fun toCodeBasePublication3(publication2: CodeBasePublication2): CodeBasePublication3 {
        return CodeBasePublication3.builder()
                .commonData(publication2.commonData)
                .entries(
                        publication2.entries.stream()
                                .map(CodeBaseEntry3::fromFormat2)
                                .collect(Collectors.toList())
                )
                .build()
    }

    private fun isValidObject(obj: Any): Boolean {
        val violations = validator.validate(obj)
        for (v in violations) {
            logger.error(
                    "Invalid {}: {}={}: {}",
                    obj.javaClass.simpleName,
                    v.propertyPath,
                    v.invalidValue,
                    v.message
            )
        }
        return violations.isEmpty()
    }
}
