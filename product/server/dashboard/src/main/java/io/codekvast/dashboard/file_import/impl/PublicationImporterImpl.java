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
package io.codekvast.dashboard.file_import.impl;

import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.lock.Lock;
import io.codekvast.common.lock.LockTemplate;
import io.codekvast.common.lock.LockTimeoutException;
import io.codekvast.common.messaging.CorrelationIdHolder;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.file_import.CodeBaseImporter;
import io.codekvast.dashboard.file_import.InvocationDataImporter;
import io.codekvast.dashboard.file_import.PublicationImporter;
import io.codekvast.javaagent.model.v2.CodeBasePublication2;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import io.codekvast.javaagent.model.v3.CodeBaseEntry3;
import io.codekvast.javaagent.model.v3.CodeBasePublication3;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Importer for serialized publications.
 *
 * <p>Deserialize the object and dispatch to the specialized importer.
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
  private final AgentService agentService;
  private final LockTemplate lockTemplate;

  @Override
  @SneakyThrows
  public boolean importPublicationFile(File file) {
    return lockTemplate.doWithLock(
        Lock.forPublication(file),
        () -> doImportPublicationFile(file),
        () -> {
          logger.info(
              "Processing of {} was already in progress in another transaction", file.getName());
          return false;
        });
  }

  private boolean doImportPublicationFile(File file) {
    logger.info("Processing {}", file.getName());
    boolean handled;
    CorrelationIdHolder.set(agentService.getCorrelationIdFromPublicationFile(file));

    try (ObjectInputStream ois =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {

      long startedAt = System.currentTimeMillis();
      Object object = ois.readObject();
      logger.debug(
          "Deserialized a {} in {} ms",
          object.getClass().getSimpleName(),
          System.currentTimeMillis() - startedAt);

      handled = !isValidObject(object) || handlePublication(object);
    } catch (LockTimeoutException | DataAccessException e) {
      // A new attempt to process the file should be made in a new transaction.
      logger.warn("Could not import {}: {}. Will try again.", file, e.toString());
      handled = false;
    } catch (InvalidClassException e) {
      // An incompatible publication file was lying in the queue.
      // The publication data is lost.
      // Prevent the file from being processed again.
      logger.error("Could not import {}: {}. Will not try again.", file, e.toString());
      handled = true;
    } catch (LicenseViolationException e) {
      // Prevent the file from being processed again.
      // The agent will keep retrying uploading new publication files.
      logger.warn("Ignoring {}: {}", file, e.toString());
      handled = true;
    } catch (Exception e) {
      // A new attempt to process the file should be made.
      // Perhaps after deploying a new version of the service.
      logger.error("Could not import " + file + ". Will try again.", e);
      handled = false;
    } finally {
      CorrelationIdHolder.clear();
    }
    return handled;
  }

  @SuppressWarnings({"InstanceofConcreteClass", "CastToConcreteClass", "ChainOfInstanceofChecks"})
  private boolean handlePublication(Object object) throws Exception {
    if (object instanceof CodeBasePublication2) {
      return codeBaseImporter.importPublication(
          toCodeBasePublication3((CodeBasePublication2) object));
    }

    if (object instanceof CodeBasePublication3) {
      return codeBaseImporter.importPublication((CodeBasePublication3) object);
    }

    if (object instanceof InvocationDataPublication2) {
      return invocationDataImporter.importPublication((InvocationDataPublication2) object);
    }

    logger.warn("Don't know how to handle {}", object.getClass().getName());
    return false;
  }

  private CodeBasePublication3 toCodeBasePublication3(CodeBasePublication2 publication2) {
    return CodeBasePublication3.builder()
        .commonData(publication2.getCommonData())
        .entries(
            publication2.getEntries().stream()
                .map(CodeBaseEntry3::fromFormat2)
                .collect(Collectors.toList()))
        .build();
  }

  private boolean isValidObject(Object object) {
    Set<ConstraintViolation<Object>> violations = validator.validate(object);
    for (ConstraintViolation<Object> v : violations) {
      logger.error(
          "Invalid {}: {}={}: {}",
          object.getClass().getSimpleName(),
          v.getPropertyPath(),
          v.getInvalidValue(),
          v.getMessage());
    }
    return violations.isEmpty();
  }
}
