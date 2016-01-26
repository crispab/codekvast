/**
 * Copyright (c) 2015-2016 Crisp AB
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
package se.crisp.codekvast.agent.lib.model.v1;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.zip.ZipEntry;

/**
 * The names of the different entries in the daemon export file.
 *
 * @author olle.hallin@crisp.se
 */
@RequiredArgsConstructor
@Getter
public enum ExportFileEntry {
    META_INFO("meta-info.properties"),
    APPLICATIONS("applications.csv"),
    METHODS("methods.csv"),
    JVMS("jvms.csv"),
    INVOCATIONS("invocations.csv");

    private final String entryName;

    public ZipEntry toZipEntry() {
        return new ZipEntry(entryName);
    }

    public static ExportFileEntry fromString(String s) {
        for (ExportFileEntry entry : values()) {
            if (entry.entryName.equals(s)) {
                return entry;
            }
        }
        throw new IllegalArgumentException("Unrecognized export file entry: " + s);
    }
}
