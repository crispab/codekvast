/*
 * Copyright (c) 2015-2017 Crisp AB
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
package io.codekvast.warehouse.file_import.impl;

import io.codekvast.javaagent.model.v1.CodeBasePublication;
import io.codekvast.javaagent.model.v1.InvocationDataPublication;
import io.codekvast.warehouse.file_import.CodeBaseImporter;
import io.codekvast.warehouse.file_import.InvocationDataImporter;
import io.codekvast.warehouse.file_import.PublicationImporter;
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
 * It deserializes the object and dispatches to the specialized importer.
 *
 * @author olle.hallin@crisp.se
 * @see CodeBasePublication
 * @see InvocationDataPublication
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
            log.debug("Deserialized a {} in {} ms", object.getClass().getSimpleName(), System.currentTimeMillis() - startedAt);

            return !isValidObject(object) || handlePublication(object);

        } catch (ClassNotFoundException | IOException e) {
            log.error("Cannot import " + file, e);
        }
        return false;
    }

    @SuppressWarnings({"InstanceofConcreteClass", "CastToConcreteClass", "ChainOfInstanceofChecks"})
    private boolean handlePublication(Object object) {
        if (object instanceof CodeBasePublication) {
            return codeBaseImporter.importPublication((CodeBasePublication) object);
        }

        if (object instanceof InvocationDataPublication) {
            return invocationDataImporter.importPublication((InvocationDataPublication) object);
        }

        log.warn("Don't know how to handle {}", object.getClass().getSimpleName());
        return false;
    }

    private boolean isValidObject(Object object) {
        Set<ConstraintViolation<Object>> violations = validator.validate(object);
        for (ConstraintViolation<Object> v : violations) {
            log.error("Invalid {}: {}={}: {}", object.getClass().getSimpleName(), v.getPropertyPath(),
                      v.getInvalidValue(), v.getMessage());
        }
        return violations.isEmpty();
    }

}
