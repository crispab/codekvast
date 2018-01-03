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

import io.codekvast.dashboard.customer.LicenseViolationException;
import io.codekvast.dashboard.file_import.CodeBaseImporter;
import io.codekvast.dashboard.file_import.InvocationDataImporter;
import io.codekvast.dashboard.file_import.PublicationImporter;
import io.codekvast.javaagent.model.v1.CodeBasePublication;
import io.codekvast.javaagent.model.v1.InvocationDataPublication;
import io.codekvast.javaagent.model.v2.CodeBasePublication2;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.*;
import java.util.Set;

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
public class PublicationImporterImpl implements PublicationImporter {

    private final CodeBaseImporter codeBaseImporter;
    private final InvocationDataImporter invocationDataImporter;
    private final Validator validator;

    @Inject
    public PublicationImporterImpl(CodeBaseImporter codeBaseImporter,
                                   InvocationDataImporter invocationDataImporter,
                                   Validator validator) {
        this.codeBaseImporter = codeBaseImporter;
        this.invocationDataImporter = invocationDataImporter;
        this.validator = validator;
    }

    @Override
    public boolean importPublicationFile(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {

            long startedAt = System.currentTimeMillis();
            Object object = ois.readObject();
            logger.debug("Deserialized a {} in {} ms", object.getClass().getSimpleName(), System.currentTimeMillis() - startedAt);

            return !isValidObject(object) || handlePublication(object);

        } catch (LicenseViolationException e) {
            logger.warn("Ignoring " + file + ": " + e);

            // Prevent the file from being processed again.
            // The agent will keep retrying uploading new publication files.
            return true;
        } catch (ClassNotFoundException | IOException e) {
            logger.error("Cannot import " + file, e);
        }
        return false;
    }

    @SuppressWarnings({"InstanceofConcreteClass", "CastToConcreteClass", "ChainOfInstanceofChecks", "deprecation"})
    private boolean handlePublication(Object object) {
        if (object instanceof CodeBasePublication) {
            return codeBaseImporter.importPublication(CodeBasePublication2.fromV1Format((CodeBasePublication) object));
        }

        if (object instanceof CodeBasePublication2) {
            return codeBaseImporter.importPublication((CodeBasePublication2) object);
        }

        if (object instanceof InvocationDataPublication) {
            return invocationDataImporter.importPublication(InvocationDataPublication2.fromV1Format((InvocationDataPublication) object));
        }

        if (object instanceof InvocationDataPublication2) {
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
