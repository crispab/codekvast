/*
 * Copyright (c) 2015 Crisp AB
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

package se.crisp.codekvast.daemon.worker.local_warehouse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.daemon.worker.DataImportException;
import se.crisp.codekvast.daemon.worker.DataImporter;

import static se.crisp.codekvast.daemon.DaemonConstants.LOCAL_WAREHOUSE_PROFILE;

/**
 * A dummy implementation of DataExporter that does nothing. In the HTTP POST profile, data is continuously uploaded to a central data
 * warehouse.
 */
@Component
@Profile(LOCAL_WAREHOUSE_PROFILE)
@Slf4j
public class LocalWarehouseDataImporterImpl implements DataImporter {

    @Override
    public void importData() throws DataImportException {
        log.trace("Data import not supported in {} profile", LOCAL_WAREHOUSE_PROFILE);
    }
}
