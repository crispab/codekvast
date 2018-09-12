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
package io.codekvast.dashboard.file_import.impl;

import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.dashboard.file_import.CodeBaseImporter;
import io.codekvast.dashboard.file_import.InvocationDataImporter;
import io.codekvast.dashboard.file_import.PublicationImporter;
import io.codekvast.dashboard.metrics.MetricsService;
import io.codekvast.javaagent.model.v1.CodeBasePublication;
import io.codekvast.javaagent.model.v1.InvocationDataPublication;
import io.codekvast.javaagent.model.v2.CodeBasePublication2;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Set;

import static io.codekvast.dashboard.metrics.MetricsService.PublicationKind.CODEBASE;
import static io.codekvast.dashboard.metrics.MetricsService.PublicationKind.INVOCATIONS;

/**
 * Importer for serialized publications.
 *
 * Deserialize the object and dispatch to the specialized importer.
 *
 * @author olle.hallin@crisp.se
 * @see CodeBasePublication2
 * @see InvocationDataPublication2
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PublicationImporterImpl implements PublicationImporter {

    private final CodeBaseImporter codeBaseImporter;
    private final InvocationDataImporter invocationDataImporter;
    private final Validator validator;
    private final MetricsService metricsService;

    @Override
    public boolean importPublicationFile(File file) {
        logger.info("Processing {}", file);
        boolean handled;
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {

            long startedAt = System.currentTimeMillis();
            Object object = ois.readObject();
            logger.debug("Deserialized a {} in {} ms", object.getClass().getSimpleName(), System.currentTimeMillis() - startedAt);

            handled = !isValidObject(object) || handlePublication(object);
        } catch (LicenseViolationException e) {
            logger.warn("Ignoring " + file + ": " + e);

            // Prevent the file from being processed again.
            // The agent will keep retrying uploading new publication files.
            handled = true;
        } catch (DataAccessException e) {
            logger.warn("Could not import {}: {}", file, e.toString());
            // A new attempt to process the file should be made in a new transaction.
            handled = false;
        } catch (Exception e) {
            logger.error("Cannot import " + file, e);
            // A new attempt to process the file should be made.
            // Perhaps after deploying a new version of the service.
            handled = false;
        }
        if (!handled) {
            metricsService.countRejectedPublication();
        }
        return handled;
    }

    @SuppressWarnings({"InstanceofConcreteClass", "CastToConcreteClass", "ChainOfInstanceofChecks", "deprecation"})
    private boolean handlePublication(Object object) {
        if (object instanceof CodeBasePublication) {
            metricsService.countImportedPublication(CODEBASE, "v1");
            return codeBaseImporter.importPublication(CodeBasePublication2.fromV1Format((CodeBasePublication) object));
        }

        if (object instanceof CodeBasePublication2) {
            metricsService.countImportedPublication(CODEBASE, "v2");
            return codeBaseImporter.importPublication((CodeBasePublication2) object);
        }

        if (object instanceof InvocationDataPublication) {
            metricsService.countImportedPublication(INVOCATIONS, "v1");
            return invocationDataImporter.importPublication(InvocationDataPublication2.fromV1Format((InvocationDataPublication) object));
        }

        if (object instanceof InvocationDataPublication2) {
            metricsService.countImportedPublication(INVOCATIONS, "v2");
            return invocationDataImporter.importPublication((InvocationDataPublication2) object);
        }

        logger.warn("Don't know how to handle {}", object.getClass().getSimpleName());
        return false;
    }

    private boolean isValidObject(Object object) {
        Set<ConstraintViolation<Object>> violations = validator.validate(object);
        for (ConstraintViolation<Object> v : violations) {
            logger.error("Invalid {}: {}={}: {}", object.getClass().getSimpleName(), v.getPropertyPath(),
                         v.getInvalidValue(), v.getMessage());
        }
        return violations.isEmpty();
    }

}
