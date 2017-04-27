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

import io.codekvast.agent.lib.model.v1.CodeBasePublication;
import io.codekvast.agent.lib.model.v1.InvocationDataPublication;
import io.codekvast.warehouse.file_import.CodeBaseImporter;
import io.codekvast.warehouse.file_import.InvocationDataImporter;
import io.codekvast.warehouse.file_import.PublicationFileImporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.*;

/**
 * Importer for serialized publications.
 *
 * @author olle.hallin@crisp.se
 * @see io.codekvast.agent.lib.model.v1.CodeBasePublication
 * @see io.codekvast.agent.lib.model.v1.InvocationDataPublication
 */
@Service
@Slf4j
public class PublicationFileImporterImpl implements PublicationFileImporter {

    private final CodeBaseImporter codeBaseImporter;
    private final InvocationDataImporter invocationDataImporter;

    @Inject
    public PublicationFileImporterImpl(CodeBaseImporter codeBaseImporter,
                                       InvocationDataImporter invocationDataImporter) {
        this.codeBaseImporter = codeBaseImporter;
        this.invocationDataImporter = invocationDataImporter;
    }

    @Override
    public boolean importPublicationFile(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {

            long startedAt = System.currentTimeMillis();
            Object object = ois.readObject();
            log.debug("Deserialized {} in {} ms", object.getClass().getSimpleName(), System.currentTimeMillis() - startedAt);

            return handlePublication(object);
        } catch (ClassNotFoundException | IOException e) {
            log.error("Cannot import " + file, e);
        }
        return false;
    }

    @SuppressWarnings({"InstanceofConcreteClass", "CastToConcreteClass", "ChainOfInstanceofChecks"})
    private boolean handlePublication(Object object) {

        if (object instanceof CodeBasePublication) {
            codeBaseImporter.importPublication((CodeBasePublication) object);
            return true;
        }

        if (object instanceof InvocationDataPublication) {
            invocationDataImporter.importPublication((InvocationDataPublication) object);
            return true;
        }
        log.warn("Don't know how to handle {}", object.getClass().getSimpleName());
        return false;
    }

}
